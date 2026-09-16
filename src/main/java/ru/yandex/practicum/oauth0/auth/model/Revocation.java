package ru.yandex.practicum.oauth0.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Revocation {
    private String tokenId;
    private TokenType tokenType;
    private Instant exp;

    public enum TokenType {
        ACCESS, REFRESH
    }
}