package com.cursosdedesarrollo.websockettwitchchat.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ChannelAuthorization(
        String broadcasterId,
        String login,
        String accessToken,
        String refreshToken,
        List<String> scopes,
        Instant expiresAt,
        Status status,
        Instant validatedAt,
        Instant updatedAt) {

    public ChannelAuthorization {
        if (broadcasterId == null || broadcasterId.isBlank()) {
            throw new IllegalArgumentException("Broadcaster ID is required");
        }
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Channel login is required");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token is required");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }
        scopes = List.copyOf(Objects.requireNonNull(scopes, "Scopes are required"));
        Objects.requireNonNull(status, "Authorization status is required");
        Objects.requireNonNull(updatedAt, "Updated timestamp is required");
    }

    public enum Status {
        AUTHORIZED,
        NEEDS_REAUTHORIZATION
    }
}
