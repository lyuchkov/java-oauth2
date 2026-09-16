package ru.yandex.practicum.oauth0.auth.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.oauth0.auth.model.Role;
import ru.yandex.practicum.oauth0.auth.model.User;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByUsername(String username) {
        String sql = """
                    SELECT u.user_id, u.username, u.password_hash, u.info,
                           r.role_id, r.name AS role_name,
                           s.name AS scope_name
                    FROM oauth.users u
                    LEFT JOIN oauth.user_roles ur ON u.user_id = ur.user_id
                    LEFT JOIN oauth.roles r ON ur.role_id = r.role_id
                    LEFT JOIN oauth.role_scopes rs ON r.role_id = rs.role_id
                    LEFT JOIN oauth.scopes s ON rs.scope_id = s.scope_id
                    WHERE u.username = ?
                """;

        User user = jdbcTemplate.query(sql, userExtractor(), username);
        return Optional.ofNullable(user);
    }

    public Optional<User> findById(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }

        String sql = """
                    SELECT u.user_id, u.username, u.password_hash, u.info,
                           r.role_id, r.name AS role_name,
                           s.name AS scope_name
                    FROM users u
                    LEFT JOIN user_roles ur ON u.user_id = ur.user_id
                    LEFT JOIN roles r ON ur.role_id = r.role_id
                    LEFT JOIN role_scopes rs ON r.role_id = rs.role_id
                    LEFT JOIN scopes s ON rs.scope_id = s.scope_id
                    WHERE u.user_id = ?
                """;

        User user = jdbcTemplate.query(sql, userExtractor(), userId);
        return Optional.ofNullable(user);
    }

    private ResultSetExtractor<User> userExtractor() {
        return rs -> {
            User user = null;
            Map<Long, Role> roleMap = new HashMap<>();

            while (rs.next()) {
                if (user == null) {
                    user = User.builder()
                            .userId(rs.getLong("user_id"))
                            .username(rs.getString("username"))
                            .passwordHash(rs.getString("password_hash"))
                            .info(rs.getString("info"))
                            .roles(new HashSet<>())
                            .build();
                }

                long roleId = rs.getLong("role_id");
                if (roleId > 0) {
                    User finalUser = user;
                    Role role = roleMap.computeIfAbsent(roleId, id -> {
                        try {
                            Role newRole = Role.builder()
                                    .roleId(id)
                                    .name(rs.getString("role_name"))
                                    .scopes(new HashSet<>())
                                    .build();
                            finalUser.getRoles().add(newRole);
                            return newRole;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

                    String scopeName = rs.getString("scope_name");
                    if (scopeName != null) {
                        role.getScopes().add(scopeName);
                    }
                }
            }
            return user;
        };
    }
}
