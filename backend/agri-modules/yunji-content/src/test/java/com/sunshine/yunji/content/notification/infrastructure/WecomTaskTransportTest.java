package com.sunshine.yunji.content.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class WecomTaskTransportTest {
    @Test void taskAcceptanceIsNotDelivery() {
        var result=WecomTaskTransport.parseCreate(Map.of("errcode",0,"msgid","task-1"));
        assertEquals("AWAITING_STAFF",result.state());assertEquals("task-1",result.taskId());
        assertEquals("PARTIAL",WecomTaskTransport.parseCreate(Map.of("errcode",0,"msgid","task-2","fail_list",List.of("customer-2"))).state());
        assertEquals("UNKNOWN",WecomTaskTransport.parseCreate(Map.of("errcode",0)).state());
        assertEquals("UNKNOWN",WecomTaskTransport.parseCreate(Map.of("errcode","0","msgid","x")).state());
    }
    @Test void usesCustomerContactTaskEndpointAndDoesNotRetryUnknown() throws Exception {
        var mapper=new ObjectMapper();var count=new AtomicInteger();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/cgi-bin/externalcontact/add_msg_template",exchange->{
            count.incrementAndGet();var payload=mapper.readTree(exchange.getRequestBody());
            assertEquals("single",payload.path("chat_type").asText());assertEquals("employee",payload.path("sender").asText());
            assertEquals("customer",payload.path("external_userid").get(0).asText());
            byte[] body="{\"errcode\":0,\"msgid\":\"task\"}".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
        });server.start();
        try {var transport=new WecomTaskTransport(mapper,URI.create("http://127.0.0.1:"+server.getAddress().getPort()));
            assertEquals("AWAITING_STAFF",transport.create("local-token","employee",List.of("customer"),"合成提醒").state());assertEquals(1,count.get());
            assertThrows(IllegalArgumentException.class,()->transport.create("local-token","employee",List.of("customer","customer"),"合成提醒"));
        }finally{server.stop(0);}
    }
}
