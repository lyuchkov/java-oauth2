CREATE SCHEMA IF NOT EXISTS oauth;

SET search_path TO oauth;

CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    info JSONB
);

CREATE TABLE IF NOT EXISTS roles (
    role_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS scopes (
    scope_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE  IF NOT EXISTS role_scopes (
    role_id BIGINT REFERENCES roles(role_id) ON DELETE CASCADE,
    scope_id BIGINT REFERENCES scopes(scope_id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, scope_id)
);

CREATE TABLE  IF NOT EXISTS user_roles (
    user_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
    role_id BIGINT REFERENCES roles(role_id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS  clients (
    client_id VARCHAR(255) PRIMARY KEY,
    client_secret_hash VARCHAR(255) NOT NULL,
    aud VARCHAR(255),
    info TEXT
);

CREATE TABLE  IF NOT EXISTS  client_grants (
    client_id VARCHAR(255) REFERENCES clients(client_id) ON DELETE CASCADE,
    grant_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (client_id, grant_type)
);

CREATE TABLE  IF NOT EXISTS client_scopes (
    client_id VARCHAR(255) REFERENCES clients(client_id) ON DELETE CASCADE,
    scope_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (client_id, scope_name)
);

CREATE TABLE  IF NOT EXISTS refresh_index (
    refresh_id VARCHAR(255) PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
    client_id VARCHAR(255) REFERENCES clients(client_id) ON DELETE CASCADE,
    exp TIMESTAMP NOT NULL,
    rotated BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS  revocation (
    token_id VARCHAR(255) PRIMARY KEY,
    token_type VARCHAR(50) NOT NULL,
    exp TIMESTAMP NOT NULL
);

CREATE TABLE  IF NOT EXISTS metrics (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    user_id BIGINT,
    client_id VARCHAR(255),
    event_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details TEXT
);

CREATE TABLE IF NOT EXISTS revoked_tokens (
    token VARCHAR(2048) PRIMARY KEY,
    expiry_date TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_revoked_tokens_expiry ON revoked_tokens(expiry_date);