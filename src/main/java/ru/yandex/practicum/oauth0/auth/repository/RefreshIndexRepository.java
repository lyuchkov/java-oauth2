package ru.yandex.practicum.oauth0.auth.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.oauth0.auth.model.RefreshIndex;

import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class RefreshIndexRepository {

    private final JdbcTemplate jdbcTemplate;

    public RefreshIndexRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(RefreshIndex refreshIndex) {
        String sql = """
                    INSERT INTO oauth.refresh_index (refresh_id, user_id, client_id, exp, rotated)
                    VALUES (?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(sql,
                refreshIndex.getRefreshId(),
                refreshIndex.getUserId(),
                refreshIndex.getClientId(),
                Timestamp.from(refreshIndex.getExp()),
                refreshIndex.isRotated()
        );
    }

    public Optional<RefreshIndex> findById(String refreshId) {
        String sql = "SELECT refresh_id, user_id, client_id, exp, rotated FROM oauth.refresh_index WHERE refresh_id = ?";

        RefreshIndex result = jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return RefreshIndex.builder()
                        .refreshId(rs.getString("refresh_id"))
                        .userId(rs.getObject("user_id", Long.class))
                        .clientId(rs.getString("client_id"))
                        .exp(rs.getTimestamp("exp").toInstant())
                        .rotated(rs.getBoolean("rotated"))
                        .build();
            }
            return null;
        }, refreshId);

        return Optional.ofNullable(result);
    }

    public void update(RefreshIndex refreshIndex) {
        String sql = "UPDATE refresh_index SET rotated = ? WHERE refresh_id = ?";

        jdbcTemplate.update(sql,
                refreshIndex.isRotated(),
                refreshIndex.getRefreshId()
        );
    }

    public void deleteById(String refreshId) {
        String sql = "DELETE FROM oauth.refresh_index WHERE refresh_id = ?";
        jdbcTemplate.update(sql, refreshId);
    }

    public void deleteAllByUserIdAndClientId(Long userId, String clientId) {
        String sql = "DELETE FROM oauth.refresh_index WHERE user_id = ? AND client_id = ?";
        jdbcTemplate.update(sql, userId, clientId);
    }

    public boolean existsById(String refreshId) {
        String sql = "SELECT COUNT(*) FROM oauth.refresh_index WHERE refresh_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, refreshId);
        return count != null && count > 0;
    }
}