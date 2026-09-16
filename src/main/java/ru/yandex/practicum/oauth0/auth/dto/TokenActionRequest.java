package ru.yandex.practicum.oauth0.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TokenActionRequest {
    @JsonProperty("token")
    private String token;
}