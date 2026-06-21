SET search_path TO oauth;

INSERT INTO clients (client_id, client_secret_hash, aud, info)
VALUES (
    'payments-web-app',
    CONCAT(chr(36), '2a', chr(36), '10', chr(36), '$2a$10$YvJsfoMyU.gptXyzcNuiCe8RdqtOQHUxsyRFLgQeczifYI92/JtjG'),
    'payments-api',
    '"Main web application frontend"'
)
ON CONFLICT (client_id) DO NOTHING;

INSERT INTO clients (client_id, client_secret_hash, aud, info)
VALUES (
    'billing-service',
    CONCAT(chr(36), '2a', chr(36), '10', chr(36), '$2a$10$p0fe6JceaHABgw99z97QtOz0GGrTQppIZ8/dnBwHjX8p5QdSb2EfG'),
    'payments-api',
    '"Backend billing microservice"'
)
ON CONFLICT (client_id) DO NOTHING;


INSERT INTO client_grants (client_id, grant_type)
VALUES ('payments-web-app', 'password')
ON CONFLICT (client_id, grant_type) DO NOTHING;

INSERT INTO client_grants (client_id, grant_type)
VALUES ('payments-web-app', 'refresh_token')
ON CONFLICT (client_id, grant_type) DO NOTHING;

INSERT INTO client_grants (client_id, grant_type)
VALUES ('billing-service', 'client_credentials')
ON CONFLICT (client_id, grant_type) DO NOTHING;


INSERT INTO client_scopes (client_id, scope_name)
VALUES ('payments-web-app', 'payments:read')
ON CONFLICT (client_id, scope_name) DO NOTHING;

INSERT INTO client_scopes (client_id, scope_name)
VALUES ('payments-web-app', 'payments:write')
ON CONFLICT (client_id, scope_name) DO NOTHING;

INSERT INTO client_scopes (client_id, scope_name)
VALUES ('billing-service', 'payments:process')
ON CONFLICT (client_id, scope_name) DO NOTHING;


INSERT INTO scopes (scope_id, name)
VALUES (1, 'payments:read')
ON CONFLICT (scope_id) DO NOTHING;

INSERT INTO scopes (scope_id, name)
VALUES (2, 'payments:write')
ON CONFLICT (scope_id) DO NOTHING;

INSERT INTO scopes (scope_id, name)
VALUES (3, 'payments:process')
ON CONFLICT (scope_id) DO NOTHING;


INSERT INTO roles (role_id, name)
VALUES (1, 'ROLE_USER')
ON CONFLICT (role_id) DO NOTHING;

INSERT INTO roles (role_id, name)
VALUES (2, 'ROLE_ADMIN')
ON CONFLICT (role_id) DO NOTHING;


INSERT INTO role_scopes (role_id, scope_id)
VALUES (1, 1)
ON CONFLICT (role_id, scope_id) DO NOTHING;

INSERT INTO role_scopes (role_id, scope_id)
VALUES (2, 1)
ON CONFLICT (role_id, scope_id) DO NOTHING;

INSERT INTO role_scopes (role_id, scope_id)
VALUES (2, 2)
ON CONFLICT (role_id, scope_id) DO NOTHING;


INSERT INTO users (user_id, username, password_hash, info)
VALUES (
    1,
    'ivan_ivanov',
    CONCAT(chr(36), '2a', chr(36), '10', chr(36), '$2a$10$fRx/x30C10wwzwF1YS/52OhbgPwmoiPLwTbcfZQYb6w4GjBNp1Z52'),
    '"Active regular customer"'
)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO users (user_id, username, password_hash, info)
VALUES (
    2,
    'blocked_user',
    CONCAT(chr(36), '2a', chr(36), '10', chr(36), '$2a$10$fRx/x30C10wwzwF1YS/52OhbgPwmoiPLwTbcfZQYb6w4GjBNp1Z52'),
    '"Suspended user account"'
)
ON CONFLICT (user_id) DO NOTHING;


INSERT INTO user_roles (user_id, role_id)
VALUES (1, 1)
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
VALUES (2, 1)
ON CONFLICT (user_id, role_id) DO NOTHING;