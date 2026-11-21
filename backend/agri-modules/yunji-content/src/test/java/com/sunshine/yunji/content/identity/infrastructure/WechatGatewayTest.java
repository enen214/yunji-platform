package com.sunshine.yunji.content.identity.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.sunshine.yunji.content.shared.BusinessException;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WechatGatewayTest {
    private static final String APP = "wx0123456789abcdef";
    private static final String PHONE = "{\"errcode\":0,\"phone_info\":{\"purePhoneNumber\":\"13800001234\",\"watermark\":{\"appid\":\"" + APP + "\"}}}";
    static class Provider implements AutoCloseable {
        final HttpServer server;
        final Map<String, String> replies = new HashMap<>();
        final List<String> calls = new ArrayList<>();
        final List<String> bodies = new ArrayList<>();
        int status = 200;
        Provider() throws Exception {
            replies.put("/sns/jscode2session", "{\"openid\":\"synthetic-openid\",\"session_key\":\"never-return-me\"}");
            replies.put("/cgi-bin/stable_token", "{\"access_token\":\"synthetic-token\",\"expires_in\":7200}");
            replies.put("/wxa/business/getuserphonenumber", PHONE);
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", r -> {
                calls.add(r.getRequestURI().getPath());
                bodies.add(new String(r.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                var bytes = replies.getOrDefault(r.getRequestURI().getPath(), "{}").getBytes(StandardCharsets.UTF_8);
                r.getResponseHeaders().add("Location", "/unexpected");
                r.sendResponseHeaders(status, bytes.length);
                r.getResponseBody().write(bytes); r.close();
            });
            server.start();
        }
        WechatGateway gateway() { return new WechatGateway(new ObjectMapper(), APP, "synthetic-secret", URI.create("http://127.0.0.1:" + server.getAddress().getPort())); }
        public void close() { server.stop(0); }
    }
    @Test void phoneLoginSeparatesCodesCachesStableTokenAndReturnsOnlyMask() throws Exception {
        try (var provider = new Provider()) {
            var gateway = provider.gateway();
            var identity = gateway.exchange("identity-code", APP, "phone-code");
            assertEquals("synthetic-openid", identity.openid());
            assertEquals("138****1234", identity.phoneMasked());
            assertEquals(List.of("/sns/jscode2session", "/cgi-bin/stable_token", "/wxa/business/getuserphonenumber"), provider.calls);
            var tokenBody = new ObjectMapper().readTree(provider.bodies.get(1));
            assertFalse(tokenBody.path("force_refresh").asBoolean(true));
            assertEquals("phone-code", new ObjectMapper().readTree(provider.bodies.get(2)).path("code").asText());
            gateway.exchange("new-identity-code", APP, "new-phone-code");
            assertEquals(1, Collections.frequency(provider.calls, "/cgi-bin/stable_token"));
            assertEquals(2, Collections.frequency(provider.calls, "/wxa/business/getuserphonenumber"));
            assertFalse(identity.toString().contains("138"));
            assertFalse(identity.toString().contains("openid"));
        }
    }
    @Test void ordinaryLoginNeverRequestsPhoneOrApplicationToken() throws Exception {
        try (var provider = new Provider()) {
            assertNull(provider.gateway().exchange("identity-code", APP).phoneMasked());
            assertEquals(List.of("/sns/jscode2session"), provider.calls);
        }
    }
    @Test void wrongAppAndMissingConfigurationNeverReachProvider() throws Exception {
        try (var provider = new Provider()) {
            assertEquals(422, assertThrows(BusinessException.class, () -> provider.gateway().exchange("code", "wrong", "phone")).status);
            assertTrue(provider.calls.isEmpty());
            assertEquals(503, assertThrows(BusinessException.class, () -> new WechatGateway(new ObjectMapper(), "", "").exchange("code", APP)).status);
        }
    }
    @Test void invalidIdentityDoesNotConsumePhoneQuota() throws Exception {
        try (var provider = new Provider()) {
            provider.replies.put("/sns/jscode2session", "{\"errcode\":40029,\"errmsg\":\"sensitive-provider-value\"}");
            var error = assertThrows(BusinessException.class, () -> provider.gateway().exchange("code", APP, "phone"));
            assertEquals(401, error.status); assertFalse(error.getMessage().contains("sensitive"));
            assertEquals(1, provider.calls.size());
        }
    }
    @Test void expiredPhoneCodeFailsWithoutSilentFallbackOrRetry() throws Exception {
        try (var provider = new Provider()) {
            provider.replies.put("/wxa/business/getuserphonenumber", "{\"errcode\":40029}");
            assertEquals(401, assertThrows(BusinessException.class, () -> provider.gateway().exchange("code", APP, "phone")).status);
            assertEquals(1, Collections.frequency(provider.calls, "/wxa/business/getuserphonenumber"));
        }
    }
    @Test void wrongWatermarkOrMalformedPhoneFailsClosed() throws Exception {
        try (var provider = new Provider()) {
            provider.replies.put("/wxa/business/getuserphonenumber", PHONE.replace(APP, "other-app"));
            assertThrows(BusinessException.class, () -> provider.gateway().exchange("code", APP, "phone"));
            provider.replies.put("/wxa/business/getuserphonenumber", PHONE.replace("13800001234", "not-a-number"));
            assertThrows(BusinessException.class, () -> provider.gateway().exchange("code2", APP, "phone2"));
            provider.replies.put("/wxa/business/getuserphonenumber", PHONE.replace("13800001234", "1234567"));
            assertEquals("1****67", provider.gateway().exchange("code3", APP, "phone3").phoneMasked());
        }
    }
    @Test void tokenFailureDoesNotCallPhoneAndExpiredTokenRefreshesOnlyOnNextAttempt() throws Exception {
        try (var provider = new Provider()) {
            var gateway = provider.gateway();
            provider.replies.put("/cgi-bin/stable_token", "{\"errcode\":40164,\"errmsg\":\"ip-not-allowed\"}");
            assertThrows(BusinessException.class, () -> gateway.exchange("code", APP, "phone"));
            assertEquals(0, Collections.frequency(provider.calls, "/wxa/business/getuserphonenumber"));
            provider.replies.put("/cgi-bin/stable_token", "{\"access_token\":\"token\",\"expires_in\":7200}");
            provider.replies.put("/wxa/business/getuserphonenumber", "{\"errcode\":42001}");
            assertThrows(BusinessException.class, () -> gateway.exchange("code2", APP, "phone2"));
            assertEquals(1, Collections.frequency(provider.calls, "/wxa/business/getuserphonenumber"));
            provider.replies.put("/wxa/business/getuserphonenumber", PHONE);
            assertNotNull(gateway.exchange("code3", APP, "phone3").phoneMasked());
            assertEquals(3, Collections.frequency(provider.calls, "/cgi-bin/stable_token"));
        }
    }
    @Test void redirectsOversizedAndMalformedResponsesAreRejected() throws Exception {
        try (var provider = new Provider()) {
            provider.status = 302;
            assertThrows(BusinessException.class, () -> provider.gateway().exchange("code", APP));
            assertEquals(1, provider.calls.size());
            provider.status = 200;
            for (String body : List.of("x".repeat(17000), "not-json", "null", "[]", "{\"errcode\":\"0\",\"openid\":\"id\"}")) {
                provider.replies.put("/sns/jscode2session", body);
                assertThrows(BusinessException.class, () -> provider.gateway().exchange("code", APP));
            }
        }
    }
}
