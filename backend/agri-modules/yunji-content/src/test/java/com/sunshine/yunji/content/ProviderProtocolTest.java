package com.sunshine.yunji.content;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunshine.yunji.content.notification.infrastructure.ProviderProtocol;
import static com.sunshine.yunji.content.notification.infrastructure.ProviderProtocol.Outcome.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;

class ProviderProtocolTest {
    ObjectMapper mapper = new ObjectMapper();
    @Test void explicitAcceptanceIsSent(){assertEquals(SENT,ProviderProtocol.sendReply(200,"{\"errcode\":0}",mapper,false).outcome());}
    @Test void lostOrMalformedRepliesFreeze(){for(String body:List.of("{}","broken","{\"errcode\":\"0\"}","{\"errcode\":999999}"))assertEquals(UNKNOWN,ProviderProtocol.sendReply(200,body,mapper,false).outcome());assertEquals(UNKNOWN,ProviderProtocol.sendReply(503,"{\"errcode\":0}",mapper,false).outcome());}
    @Test void rejectionAndRetryHaveDifferentQuotaSemantics(){assertEquals(PERMANENT_FAILURE,ProviderProtocol.sendReply(200,"{\"errcode\":43101}",mapper,false).outcome());assertEquals(RETRYABLE_FAILURE,ProviderProtocol.sendReply(200,"{\"errcode\":42001}",mapper,false).outcome());}
    @Test void partialEnterpriseRecipientsNeverTriggerBlindRetry(){assertEquals(UNKNOWN,ProviderProtocol.sendReply(200,"{\"errcode\":0,\"invaliduser\":\"user2\"}",mapper,true).outcome());}
    @Test void callbackAuthenticityAndFreshness() throws Exception {
        var now=Instant.parse("2026-10-05T00:00:00Z");String stamp=Long.toString(now.getEpochSecond());String[] parts={"token",stamp,"nonce","cipher"};Arrays.sort(parts);
        var signature=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-1").digest(String.join("",parts).getBytes(StandardCharsets.UTF_8)));
        assertDoesNotThrow(()->ProviderProtocol.verifySignature("token",stamp,"nonce","cipher",signature,now));
        assertThrows(IllegalArgumentException.class,()->ProviderProtocol.verifySignature("token",stamp,"nonce","tampered",signature,now));
        assertThrows(IllegalArgumentException.class,()->ProviderProtocol.verifySignature("token",stamp,"nonce","cipher",signature,now.plusSeconds(301)));
    }
    @Test void authenticatedEncryptionChecksReceiverAndPadding() throws Exception {
        byte[] key=new byte[32];Arrays.fill(key,(byte)7);String encoding=Base64.getEncoder().withoutPadding().encodeToString(key);
        byte[] xml="<xml><Event>test</Event></xml>".getBytes(StandardCharsets.UTF_8),receiver="app-id".getBytes(StandardCharsets.UTF_8);
        int size=20+xml.length+receiver.length,pad=32-size%32;byte[] plain=new byte[size+pad];ByteBuffer.wrap(plain).position(16).putInt(xml.length).put(xml).put(receiver);Arrays.fill(plain,size,plain.length,(byte)pad);
        var cipher=Cipher.getInstance("AES/CBC/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new IvParameterSpec(Arrays.copyOf(key,16)));
        String encrypted=Base64.getEncoder().encodeToString(cipher.doFinal(plain));
        assertEquals(new String(xml,StandardCharsets.UTF_8),ProviderProtocol.decrypt(encoding,encrypted,"app-id"));
        assertThrows(IllegalArgumentException.class,()->ProviderProtocol.decrypt(encoding,encrypted,"other-app"));
        plain[plain.length-1]=0;String bad=Base64.getEncoder().encodeToString(cipher.doFinal(plain));
        assertThrows(IllegalArgumentException.class,()->ProviderProtocol.decrypt(encoding,bad,"app-id"));
    }
    @Test void externalEntitiesAndDuplicateFieldsAreRejected(){
        assertEquals("subscribe",ProviderProtocol.callbackFields("<xml><Event><![CDATA[subscribe]]></Event></xml>").get("Event"));
        assertThrows(IllegalArgumentException.class,()->ProviderProtocol.callbackFields("<!DOCTYPE xml [<!ENTITY x SYSTEM 'file:///C:/Windows/win.ini'>]><xml><Event>&x;</Event></xml>"));
        assertThrows(IllegalArgumentException.class,()->ProviderProtocol.callbackFields("<xml><Event>a</Event><Event>b</Event></xml>"));
    }
}
