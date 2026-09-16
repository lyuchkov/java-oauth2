package ru.yandex.practicum.oauth0.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Client {
    private String clientId;
    private String clientSecretHash;
    private String aud;
    private String info;
    private Set<String> authorizedGrantTypes;
    private Set<String> scopes;
}