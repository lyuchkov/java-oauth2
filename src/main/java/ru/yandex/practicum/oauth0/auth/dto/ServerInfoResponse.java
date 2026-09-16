package ru.yandex.practicum.oauth0.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServerInfoResponse {
    private String issuer;
    private String aud;
    @JsonProperty("access_ttl_sec")
    private int accessTtlSec;
    @JsonProperty("refresh_ttl_days")
    private int refreshTtlDays;
    @JsonProperty("token_alg")
    private String tokenAlg;
}