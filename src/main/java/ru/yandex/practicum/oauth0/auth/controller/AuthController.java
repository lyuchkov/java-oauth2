package ru.yandex.practicum.oauth0.auth.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.oauth0.auth.dto.TokenRequest;
import ru.yandex.practicum.oauth0.auth.dto.TokenResponse;
import ru.yandex.practicum.oauth0.auth.service.AuthService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public TokenResponse token(@RequestHeader(value = "Authorization", required = false) String authHeader,
                               @RequestParam Map<String, String> parameters) {

        String clientId = parameters.get("client_id");
        String clientSecret = parameters.get("client_secret");

        if (authHeader != null && authHeader.toLowerCase().startsWith("basic ")) {
            String base64Credentials = authHeader.substring(6).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            String[] values = credentials.split(":", 2);
            clientId = values[0];
            clientSecret = values[1];
        }

        TokenRequest request = new TokenRequest();
        request.setClientId(clientId);
        request.setClientSecret(clientSecret);
        request.setGrantType(parameters.get("grant_type"));
        request.setUsername(parameters.get("username"));
        request.setPassword(parameters.get("password"));

        String scopes = parameters.get("scope");
        if (scopes != null && !scopes.isBlank()) {
            request.setScopes(Set.of(scopes.split(" ")));
        }

        if ("refresh_token".equals(request.getGrantType())) {
            return authService.refreshToken(parameters.get("refresh_token"), clientId, clientSecret);
        }

        return authService.issueToken(request);
    }

    @PostMapping("/revoke")
    public void revoke(@RequestParam("token") String tokenStr) {
        authService.revokeToken(tokenStr);
    }

    @PostMapping("/introspect")
    public Map<String, Object> introspect(@RequestParam("token") String tokenStr) {
        return authService.introspectToken(tokenStr);
    }
}