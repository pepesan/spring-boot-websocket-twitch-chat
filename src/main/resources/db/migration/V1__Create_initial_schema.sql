CREATE TABLE IF NOT EXISTS channel_authorizations (
    provider_id TEXT NOT NULL,
    provider_user_id TEXT NOT NULL,
    login TEXT NOT NULL,
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    scopes TEXT NOT NULL,
    expires_at BIGINT NULL,
    status TEXT NOT NULL,
    validated_at BIGINT NULL,
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (provider_id, provider_user_id)
);

CREATE TABLE IF NOT EXISTS command (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(255),
    output_text VARCHAR(255),
    output_type VARCHAR(255),
    owner_only BOOLEAN,
    only_mods BOOLEAN
);

CREATE TABLE IF NOT EXISTS command_seq (
    sequence_name VARCHAR(255) NOT NULL PRIMARY KEY,
    next_val BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS twitch_user (
    id VARCHAR(36) PRIMARY KEY NOT NULL,
    login VARCHAR(255),
    display_name VARCHAR(255),
    type VARCHAR(255),
    broadcaster_type VARCHAR(255),
    description VARCHAR(255),
    profile_image_url VARCHAR(255),
    offline_image_url VARCHAR(255),
    email VARCHAR(255),
    created_at TIMESTAMP
);
