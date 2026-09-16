package ru.yandex.practicum.oauth0.auth.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class RevokedTokenRepository {

    private final JdbcTemplate jdbcTemplate;

    public RevokedTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(String token, Instant exp) {
        String sql = "INSERT INTO oauth.revoked_tokens (token, expiry_date) VALUES (?, ?) ON CONFLICT DO NOTHING";
        jdbcTemplate.update(sql, token, Timestamp.from(exp));
    }

    public boolean isRevoked(String token) {
        String sql = "SELECT COUNT(*) FROM oauth.revoked_tokens WHERE token = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, token);
        return count != null && count > 0;
    }
}