package com.cursosdedesarrollo.websockettwitchchat.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ChannelAuthorizationTest {
    @Test
    void rejectsMissingChannelIdentityOrTokens() {
        assertThrows(IllegalArgumentException.class, () -> authorization(" ", "streamer", "access", "refresh"));
        assertThrows(IllegalArgumentException.class, () -> authorization("1001", " ", "access", "refresh"));
        assertThrows(IllegalArgumentException.class, () -> authorization("1001", "streamer", " ", "refresh"));
        assertThrows(IllegalArgumentException.class, () -> authorization("1001", "streamer", "access", " "));
    }

    private static ChannelAuthorization authorization(String broadcasterId, String login, String accessToken, String refreshToken) {
        return new ChannelAuthorization(
                broadcasterId, login, accessToken, refreshToken, List.of(), null,
                ChannelAuthorization.Status.AUTHORIZED, null, Instant.parse("2026-01-01T00:00:00Z"));
    }
}
