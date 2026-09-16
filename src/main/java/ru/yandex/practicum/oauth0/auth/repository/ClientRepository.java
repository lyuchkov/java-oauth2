package ru.yandex.practicum.oauth0.auth.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.oauth0.auth.model.Client;

import java.util.HashSet;
import java.util.Optional;

@Repository
public class ClientRepository {

    private final JdbcTemplate jdbcTemplate;

    public ClientRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Client> findById(String clientId) {
        String sql = """
                    SELECT c.client_id, c.client_secret_hash, c.aud, c.info,
                           cg.grant_type,
                           cs.scope_name
                    FROM oauth.clients c
                    LEFT JOIN oauth.client_grants cg ON c.client_id = cg.client_id
                    LEFT JOIN oauth.client_scopes cs ON c.client_id = cs.client_id
                    WHERE c.client_id = ?
                """;

        Client client = jdbcTemplate.query(sql, clientExtractor(), clientId);
        return Optional.ofNullable(client);
    }

    private ResultSetExtractor<Client> clientExtractor() {
        return rs -> {
            Client client = null;

            while (rs.next()) {
                if (client == null) {
                    client = Client.builder()
                            .clientId(rs.getString("client_id"))
                            .clientSecretHash(rs.getString("client_secret_hash"))
                            .aud(rs.getString("aud"))
                            .info(rs.getString("info"))
                            .authorizedGrantTypes(new HashSet<>())
                            .scopes(new HashSet<>())
                            .build();
                }

                String grantType = rs.getString("grant_type");
                if (grantType != null) {
                    client.getAuthorizedGrantTypes().add(grantType);
                }

                String scopeName = rs.getString("scope_name");
                if (scopeName != null) {
                    client.getScopes().add(scopeName);
                }
            }
            return client;
        };
    }
}