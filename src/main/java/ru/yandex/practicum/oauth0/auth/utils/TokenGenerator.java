package ru.yandex.practicum.oauth0.auth.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.oauth0.auth.model.Client;
import ru.yandex.practicum.oauth0.auth.model.User;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class TokenGenerator {

    private final String secret;
    private final String issuer;
    private final ObjectMapper objectMapper;

    public TokenGenerator(
            @Value("${auth.jwt.secret:super-secret-key-for-hs256-algorithm-123456}") String secret,
            @Value("${auth.jwt.issuer:mini-auth}") String issuer,
            ObjectMapper objectMapper) {
        this.secret = secret;
        this.issuer = issuer;
        this.objectMapper = objectMapper;
    }

    public String generateAccessToken(User user, Client client, Set<String> scopes, long ttlSeconds) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("iss", issuer);
        payloadMap.put("aud", client.getAud());
        payloadMap.put("iat", now.getEpochSecond());
        payloadMap.put("exp", exp.getEpochSecond());
        payloadMap.put("client_id", client.getClientId());
        payloadMap.put("scope", String.join(" ", scopes));

        if (user != null) {
            payloadMap.put("sub", String.valueOf(user.getUserId()));
            payloadMap.put("preferred_username", user.getUsername());
        }

        return encode(payloadMap);
    }

    public String generateRefreshToken(String refreshId) {
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("jti", refreshId);
        payloadMap.put("iss", issuer);
        payloadMap.put("iat", Instant.now().getEpochSecond());

        return encode(payloadMap);
    }


    private String encode(Map<String, Object> payloadMap) {
        try {
            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String payloadJson = objectMapper.writeValueAsString(payloadMap);

            String header = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
            String payload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

            String toSign = header + "." + payload;
            String signature = signHs256(toSign, secret);

            return toSign + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Error during token generation", e);
        }
    }

    private String signHs256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(key);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(rawHmac);
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] base64UrlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    public Map<String, Object> decodeAndVerify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;

            String header = parts[0];
            String payload = parts[1];
            String signature = parts[2];

            String toSign = header + "." + payload;
            String expectedSig = signHs256(toSign, secret);

            if (!expectedSig.equals(signature)) {
                return null;
            }

            String payloadJson = new String(base64UrlDecode(payload), StandardCharsets.UTF_8);

            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);
            return claims;

        } catch (Exception e) {
            return null;
        }
    }
}