package com.sunshine.yunji.content.notification.infrastructure;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class ProviderHttpTransportTest {
    @Test void sendsOnlyOnceAndKeepsServerTokenCacheSeparateFromDelivery() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var tokenCalls=new AtomicInteger();var sends=new AtomicInteger();
        server.createContext("/cgi-bin/token",r->{tokenCalls.incrementAndGet();byte[] b="{\"access_token\":\"synthetic-token\",\"expires_in\":7200}".getBytes(StandardCharsets.UTF_8);r.sendResponseHeaders(200,b.length);r.getResponseBody().write(b);r.close();});
        server.createContext("/cgi-bin/message/subscribe/send",r->{sends.incrementAndGet();assertEquals("POST",r.getRequestMethod());assertTrue(r.getRequestURI().getRawQuery().contains("synthetic-token"));String content=new String(r.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);assertTrue(content.contains("synthetic-recipient"));byte[] b="{\"errcode\":0}".getBytes(StandardCharsets.UTF_8);r.sendResponseHeaders(200,b.length);r.getResponseBody().write(b);r.close();});
        server.start();try{
            var uri=URI.create("http://127.0.0.1:"+server.getAddress().getPort());var t=new ProviderHttpTransport(new ObjectMapper(),uri,uri);var channel=ProviderHttpTransport.Channel.WECHAT_SUBSCRIBE;
            var token=t.accessToken(channel,"synthetic-app","synthetic-secret");assertEquals(token,t.accessToken(channel,"synthetic-app","synthetic-secret"));assertEquals(1,tokenCalls.get());
            assertEquals(ProviderProtocol.Outcome.SENT,t.send(channel,token,Map.of("touser","synthetic-recipient")).outcome());assertEquals(1,sends.get());
            t.invalidateToken(channel,"synthetic-app","synthetic-secret");t.accessToken(channel,"synthetic-app","synthetic-secret");assertEquals(2,tokenCalls.get());
        }finally{server.stop(0);}
    }
    @Test void redirectsAndDroppedResponsesFreezeWithoutFollowingOrRetrying() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var hits=new AtomicInteger();
        server.createContext("/cgi-bin/message/send",r->{hits.incrementAndGet();r.getResponseHeaders().add("Location","/unexpected");r.sendResponseHeaders(302,-1);r.close();});
        server.createContext("/unexpected",r->{fail("Redirect must not be followed");});server.start();
        try{var uri=URI.create("http://127.0.0.1:"+server.getAddress().getPort());var t=new ProviderHttpTransport(new ObjectMapper(),uri,uri);assertEquals(ProviderProtocol.Outcome.UNKNOWN,t.send(ProviderHttpTransport.Channel.WECOM,"synthetic",Map.of("touser","fixture")).outcome());assertEquals(1,hits.get());}
        finally{server.stop(0);}
    }
    @Test void missingCredentialsFailBeforeNetwork(){var t=new ProviderHttpTransport(new ObjectMapper());assertThrows(IllegalStateException.class,()->t.accessToken(ProviderHttpTransport.Channel.WECOM,"", ""));}
}
