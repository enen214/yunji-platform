package com.sunshine.yunji.content.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import java.io.StringReader;

/** Offline protocol primitives. These do not grant binding/quota or send a message. */
public final class ProviderProtocol {
    private ProviderProtocol() {}
    public enum Outcome { SENT, PERMANENT_FAILURE, RETRYABLE_FAILURE, UNKNOWN }
    public record Reply(Outcome outcome, String code, String messageId) {}

    public static Reply sendReply(int status, String json, ObjectMapper mapper, boolean wecom) {
        // A broken response may follow a successful send: freeze rather than send twice.
        if (status != 200 || json == null || json.length() > 65536)
            return new Reply(Outcome.UNKNOWN, "HTTP_" + status, null);
        try {
            var root = mapper.readTree(json);
            var codeNode = root.get("errcode");
            if (codeNode == null || !codeNode.isIntegralNumber() || !codeNode.canConvertToInt())
                return new Reply(Outcome.UNKNOWN, "MALFORMED_REPLY", null);
            int code = codeNode.intValue();
            if (code == 0) {
                if (wecom && Arrays.stream(new String[]{"invaliduser", "invalidparty", "invalidtag"})
                        .anyMatch(k -> root.has(k) && !root.path(k).asText().isBlank()))
                    return new Reply(Outcome.UNKNOWN, "PARTIAL_RECIPIENT_FAILURE", null);
                return new Reply(Outcome.SENT, "0", root.hasNonNull("msgid") ? root.get("msgid").asText() : null);
            }
            var outcome = switch (code) {
                case -1, 40014, 42001, 45009 -> Outcome.RETRYABLE_FAILURE;
                case 40003, 40037, 43101, 47003 -> Outcome.PERMANENT_FAILURE;
                default -> Outcome.UNKNOWN;
            };
            return new Reply(outcome, Integer.toString(code), null);
        } catch (Exception ignored) {
            return new Reply(Outcome.UNKNOWN, "MALFORMED_REPLY", null);
        }
    }

    public static void verifySignature(String token, String timestamp, String nonce, String encrypted,
                                       String signature, Instant now) {
        try {
            if (token == null || token.isBlank() || nonce == null || nonce.length() > 256
                    || timestamp == null || timestamp.length() > 20 || encrypted == null || encrypted.length() > 131072)
                throw new IllegalArgumentException();
            var sent = Instant.ofEpochSecond(Long.parseLong(timestamp));
            if (sent.isBefore(now.minusSeconds(300)) || sent.isAfter(now.plusSeconds(300)))
                throw new IllegalArgumentException();
            String[] values = {token, timestamp, nonce, encrypted};
            Arrays.sort(values);
            byte[] expected = MessageDigest.getInstance("SHA-1").digest(String.join("", values).getBytes(StandardCharsets.UTF_8));
            if (signature == null || signature.length() != 40 || !MessageDigest.isEqual(expected, HexFormat.of().parseHex(signature)))
                throw new IllegalArgumentException();
        } catch (Exception e) { throw new IllegalArgumentException("Callback signature or freshness invalid"); }
    }

    public static String decrypt(String encodingKey, String encrypted, String receiver) {
        try {
            if (encodingKey == null || encodingKey.length() != 43 || encrypted == null || encrypted.length() > 131072
                    || receiver == null || receiver.isBlank()) throw new IllegalArgumentException();
            byte[] key = Base64.getDecoder().decode(encodingKey + "=");
            byte[] bytes = Base64.getDecoder().decode(encrypted);
            if (bytes.length == 0 || bytes.length % 16 != 0) throw new IllegalArgumentException();
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(Arrays.copyOf(key, 16)));
            byte[] plain = cipher.doFinal(bytes);
            int pad = plain[plain.length - 1] & 255;
            if (pad < 1 || pad > 32 || pad > plain.length) throw new IllegalArgumentException();
            for (int i = plain.length - pad; i < plain.length; i++) if ((plain[i] & 255) != pad) throw new IllegalArgumentException();
            int end = plain.length - pad;
            if (end < 20) throw new IllegalArgumentException();
            int length = ByteBuffer.wrap(plain, 16, 4).getInt();
            if (length < 0 || length > end - 20) throw new IllegalArgumentException();
            byte[] recipient = Arrays.copyOfRange(plain, 20 + length, end);
            if (!MessageDigest.isEqual(recipient, receiver.getBytes(StandardCharsets.UTF_8))) throw new IllegalArgumentException();
            return new String(plain, 20, length, StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalArgumentException("Callback ciphertext or receiver invalid"); }
    }

    public static Map<String,String> callbackFields(String xml) {
        try {
            if (xml == null || xml.length() > 65536) throw new IllegalArgumentException();
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler(){@Override public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {throw e;} @Override public void error(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {throw e;}});
            var doc = builder.parse(new InputSource(new StringReader(xml)));
            if (!"xml".equals(doc.getDocumentElement().getTagName())) throw new IllegalArgumentException();
            var fields = new java.util.HashMap<String,String>();
            var nodes = doc.getDocumentElement().getChildNodes();
            for (int i=0; i<nodes.getLength(); i++) {
                var node = nodes.item(i);
                if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    if (fields.putIfAbsent(node.getNodeName(), node.getTextContent()) != null) throw new IllegalArgumentException();
                }
            }
            return Map.copyOf(fields);
        } catch (Exception e) { throw new IllegalArgumentException("Callback XML invalid"); }
    }
}
