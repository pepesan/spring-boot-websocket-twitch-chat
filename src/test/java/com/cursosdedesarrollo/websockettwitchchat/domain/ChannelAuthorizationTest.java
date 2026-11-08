package com.cursosdedesarrollo.websockettwitchchat.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ChannelAuthorizationTest {
    @Test
    void rejectsMissingProviderIdentityOrTokens() {
        assertThrows(IllegalArgumentException.class, () -> authorization(" ", "1001", "streamer", "access", "refresh"));
        assertThrows(IllegalArgumentException.class, () -> authorization("twitch", " ", "streamer", "access", "refresh"));
        assertThrows(IllegalArgumentException.class, () -> authorization("twitch", "1001", " ", "access", "refresh"));
        assertThrows(IllegalArgumentException.class, () -> authorization("twitch", "1001", "streamer", " ", "refresh"));
        assertThrows(IllegalArgumentException.class, () -> authorization("twitch", "1001", "streamer", "access", " "));
    }

    private static ChannelAuthorization authorization(
            String provider, String providerUserId, String login, String accessToken, String refreshToken) {
        return new ChannelAuthorization(
                provider, providerUserId, login, accessToken, refreshToken, List.of(), null,
                ChannelAuthorization.Status.AUTHORIZED, null, Instant.parse("2026-01-01T00:00:00Z"));
    }
}
