package com.cursosdedesarrollo.websockettwitchchat.services;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.repositories.SQLiteChannelAuthorizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChannelAuthorizationServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");
    private static final List<String> REQUIRED_SCOPES = List.of("chat.read", "chat.write");
    private static final OAuthTokenResponse TOKEN = new OAuthTokenResponse(
            "validated-access-token", "rotating-refresh-token", 7200, REQUIRED_SCOPES, "bearer");

    private OAuthProvider provider;
    private SQLiteChannelAuthorizationRepository repository;
    private ChannelAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        provider = mock(OAuthProvider.class);
        when(provider.providerId()).thenReturn("youtube");
        when(provider.clientId()).thenReturn("expected-client-id");
        when(provider.requiredScopes()).thenReturn(REQUIRED_SCOPES);
        repository = mock(SQLiteChannelAuthorizationRepository.class);
        authorizationService = new ChannelAuthorizationService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void persistsGenericProviderIdentityOnlyAfterValidation() {
        when(provider.validateAccessToken(TOKEN.accessToken())).thenReturn(
                new OAuthIdentity("expected-client-id", "external-user-1001", "streamer_login", REQUIRED_SCOPES, 3600));

        ChannelAuthorization authorization = authorizationService.authorize(provider, TOKEN);

        assertEquals("youtube", authorization.providerId());
        assertEquals("external-user-1001", authorization.providerUserId());
        assertEquals("streamer_login", authorization.login());
        assertEquals(TOKEN.accessToken(), authorization.accessToken());
        assertEquals(TOKEN.refreshToken(), authorization.refreshToken());
        assertEquals(NOW.plusSeconds(3600), authorization.expiresAt());
        assertEquals(NOW, authorization.validatedAt());
        verify(repository).save(authorization);
    }

    @Test
    void rejectsTokenIssuedForAnotherClientBeforePersistence() {
        when(provider.validateAccessToken(TOKEN.accessToken())).thenReturn(
                new OAuthIdentity("other-client-id", "external-user-1001", "streamer_login", REQUIRED_SCOPES, 3600));

        assertThrows(IllegalArgumentException.class, () -> authorizationService.authorize(provider, TOKEN));

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsMissingProviderScopesBeforePersistence() {
        when(provider.validateAccessToken(TOKEN.accessToken())).thenReturn(
                new OAuthIdentity("expected-client-id", "external-user-1001", "streamer_login", List.of("chat.read"), 3600));

        assertThrows(IllegalArgumentException.class, () -> authorizationService.authorize(provider, TOKEN));

        verify(repository, never()).save(any());
    }

    @Test
    void propagatesInvalidTokenWithoutPersistingIt() {
        when(provider.validateAccessToken(TOKEN.accessToken())).thenThrow(new IllegalStateException("invalid token"));

        assertThrows(IllegalStateException.class, () -> authorizationService.authorize(provider, TOKEN));

        verify(repository, never()).save(any());
    }
}
