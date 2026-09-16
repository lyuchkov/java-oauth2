package ru.yandex.practicum.oauth0.auth.service;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.oauth0.auth.dto.TokenRequest;
import ru.yandex.practicum.oauth0.auth.dto.TokenResponse;
import ru.yandex.practicum.oauth0.auth.exceptions.InvalidTokenException;
import ru.yandex.practicum.oauth0.auth.exceptions.TokenReuseException;
import ru.yandex.practicum.oauth0.auth.exceptions.UserBlockedException;
import ru.yandex.practicum.oauth0.auth.model.Client;
import ru.yandex.practicum.oauth0.auth.model.RefreshIndex;
import ru.yandex.practicum.oauth0.auth.model.User;
import ru.yandex.practicum.oauth0.auth.repository.ClientRepository;
import ru.yandex.practicum.oauth0.auth.repository.RefreshIndexRepository;
import ru.yandex.practicum.oauth0.auth.repository.RevokedTokenRepository;
import ru.yandex.practicum.oauth0.auth.repository.UserRepository;
import ru.yandex.practicum.oauth0.auth.utils.TokenGenerator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final RefreshIndexRepository refreshIndexRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final TokenGenerator tokenGenerator;

    @Value("${auth.access-ttl-sec:900}")
    private long accessTtlSec;

    @Value("${auth.refresh-ttl-days:14}")
    private long refreshTtlDays;

    public AuthService(UserRepository userRepository,
                       ClientRepository clientRepository,
                       RefreshIndexRepository refreshIndexRepository,
                       RevokedTokenRepository revokedTokenRepository,
                       TokenGenerator tokenGenerator) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.refreshIndexRepository = refreshIndexRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.tokenGenerator = tokenGenerator;
    }

    @Transactional
    public TokenResponse issueToken(TokenRequest request) {
        Client client = authenticateClient(request.getClientId(), request.getClientSecret());

        if (!client.getAuthorizedGrantTypes().contains(request.getGrantType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grant type not allowed for this client");
        }

        return switch (request.getGrantType()) {
            case "password" -> handlePasswordGrant(request, client);
            case "client_credentials" -> handleClientCredentialsGrant(request, client);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported grant_type");
        };
    }

    @Transactional
    public TokenResponse refreshToken(String refreshTokenStr, String clientId, String clientSecret) {
        Client client = authenticateClient(clientId, clientSecret);

        Map<String, Object> claims = tokenGenerator.decodeAndVerify(refreshTokenStr);
        if (claims == null || !claims.containsKey("jti")) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String refreshId = (String) claims.get("jti");

        RefreshIndex refreshIndex = refreshIndexRepository.findById(refreshId)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (!refreshIndex.getClientId().equals(client.getClientId())) {
            throw new InvalidTokenException("Token does not belong to this client");
        }

        if (refreshIndex.getExp().isBefore(Instant.now())) {
            refreshIndexRepository.deleteById(refreshId);
            throw new InvalidTokenException("Refresh token expired");
        }

        if (refreshIndex.isRotated()) {
            refreshIndexRepository.deleteAllByUserIdAndClientId(refreshIndex.getUserId(), refreshIndex.getClientId());
            throw new TokenReuseException("Token compromise detected. All sessions revoked.");
        }

        refreshIndex.setRotated(true);
        refreshIndexRepository.update(refreshIndex);

        User user = userRepository.findById(refreshIndex.getUserId())
                .orElseThrow(() -> new UserBlockedException("User no longer exists"));

        Set<String> userScopes = user.getRoles().stream()
                .flatMap(role -> role.getScopes().stream())
                .collect(Collectors.toSet());

        Set<String> finalScopes = calculateIntersectedScopes(null, client.getScopes(), userScopes);

        String newAccessToken = tokenGenerator.generateAccessToken(user, client, finalScopes, accessTtlSec);

        String newRefreshId = UUID.randomUUID().toString();
        Instant newRefreshExp = Instant.now().plus(refreshTtlDays, ChronoUnit.DAYS);

        RefreshIndex newIndex = RefreshIndex.builder()
                .refreshId(newRefreshId)
                .userId(user.getUserId())
                .clientId(client.getClientId())
                .exp(newRefreshExp)
                .rotated(false)
                .build();
        refreshIndexRepository.save(newIndex);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(tokenGenerator.generateRefreshToken(newRefreshId))
                .tokenType("Bearer")
                .expiresIn(accessTtlSec)
                .build();
    }

    @Transactional
    public void revokeToken(String tokenStr) {
        Map<String, Object> claims = tokenGenerator.decodeAndVerify(tokenStr);
        if (claims == null) {
            return;
        }

        if (claims.containsKey("jti")) {
            String refreshId = (String) claims.get("jti");
            refreshIndexRepository.deleteById(refreshId);
        } else {
            long expSeconds = ((Number) claims.get("exp")).longValue();
            Instant exp = Instant.ofEpochSecond(expSeconds);
            revokedTokenRepository.save(tokenStr, exp);
        }
    }

    public Map<String, Object> introspectToken(String tokenStr) {
        Map<String, Object> response = new HashMap<>();

        Map<String, Object> claims = tokenGenerator.decodeAndVerify(tokenStr);
        if (claims == null) {
            response.put("active", false);
            return response;
        }

        long exp = ((Number) claims.get("exp")).longValue();
        if (Instant.now().getEpochSecond() > exp) {
            response.put("active", false);
            return response;
        }

        if (claims.containsKey("jti")) {
            String refreshId = (String) claims.get("jti");
            if (!refreshIndexRepository.existsById(refreshId)) {
                response.put("active", false);
                return response;
            }
        } else {
            if (revokedTokenRepository.isRevoked(tokenStr)) {
                response.put("active", false);
                return response;
            }
        }

        response.putAll(claims);
        response.put("active", true);
        return response;
    }

    private TokenResponse handlePasswordGrant(TokenRequest request, Client client) {
        User user = authenticateUser(request.getUsername(), request.getPassword());

        Set<String> userScopes = user.getRoles().stream()
                .flatMap(role -> role.getScopes().stream())
                .collect(Collectors.toSet());

        Set<String> finalScopes = calculateIntersectedScopes(request.getScopes(), client.getScopes(), userScopes);

        String accessToken = tokenGenerator.generateAccessToken(user, client, finalScopes, accessTtlSec);

        String refreshId = UUID.randomUUID().toString();
        Instant refreshExp = Instant.now().plus(refreshTtlDays, ChronoUnit.DAYS);

        RefreshIndex refreshIndex = RefreshIndex.builder()
                .refreshId(refreshId)
                .userId(user.getUserId())
                .clientId(client.getClientId())
                .exp(refreshExp)
                .rotated(false)
                .build();
        refreshIndexRepository.save(refreshIndex);

        String refreshToken = tokenGenerator.generateRefreshToken(refreshId);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTtlSec)
                .build();
    }

    private TokenResponse handleClientCredentialsGrant(TokenRequest request, Client client) {
        Set<String> finalScopes = calculateIntersectedScopes(request.getScopes(), client.getScopes(), client.getScopes());

        String accessToken = tokenGenerator.generateAccessToken(null, client, finalScopes, accessTtlSec);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(accessTtlSec)
                .build();
    }

    private Client authenticateClient(String clientId, String clientSecret) {
        if (clientId == null || clientSecret == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing client credentials");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid client_id"));

        if (!BCrypt.checkpw(clientSecret, client.getClientSecretHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid client_secret");
        }
        return client;
    }

    private User authenticateUser(String username, String password) {
        if (username == null || password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return user;
    }

    private Set<String> calculateIntersectedScopes(Set<String> requested, Set<String> clientScopes, Set<String> userScopes) {
        Set<String> requestedScopes = (requested != null && !requested.isEmpty()) ? requested : clientScopes;

        return requestedScopes.stream()
                .filter(clientScopes::contains)
                .filter(userScopes::contains)
                .collect(Collectors.toSet());
    }
}