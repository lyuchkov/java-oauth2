package ru.yandex.practicum.oauth0.rs;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class JwtDecoder {

    private final RsProperties rsProperties;

    public JwtDecoder(RsProperties rsProperties) {
        this.rsProperties = rsProperties;
    }

    public Claims decodeAndVerify(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(rsProperties.getSecret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}