package com.cursosdedesarrollo.websockettwitchchat.auth;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.repositories.SQLiteChannelAuthorizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        when(provider.validateAccessToken(TOKEN.accessToken())).thenReturn(identity("external-user-1001"));

        ChannelAuthorization authorization = authorizationService.authorize(provider, TOKEN, 42);

        assertEquals("youtube", authorization.providerId());
        assertEquals("external-user-1001", authorization.providerUserId());
        assertEquals("streamer_login", authorization.login());
        assertEquals(TOKEN.accessToken(), authorization.accessToken());
        assertEquals(TOKEN.refreshToken(), authorization.refreshToken());
        assertEquals(NOW.plusSeconds(3600), authorization.expiresAt());
        assertEquals(NOW, authorization.validatedAt());
        verify(repository).save(authorization, 42);
    }

    @Test
    void rejectsTokenIssuedForAnotherClientBeforePersistence() {
        when(provider.validateAccessToken(TOKEN.accessToken())).thenReturn(
                new OAuthIdentity("other-client-id", "external-user-1001", "streamer_login", REQUIRED_SCOPES, 3600));

        assertThrows(IllegalArgumentException.class, () -> authorizationService.authorize(provider, TOKEN, 42));

        verify(repository, never()).save(any());
        verify(repository, never()).save(any(), any());
    }

    @Test
    void rejectsMissingProviderScopesBeforePersistence() {
        when(provider.validateAccessToken(TOKEN.accessToken())).thenReturn(
                new OAuthIdentity("expected-client-id", "external-user-1001", "streamer_login", List.of("chat.read"), 3600));

        assertThrows(IllegalArgumentException.class, () -> authorizationService.authorize(provider, TOKEN, 42));

        verify(repository, never()).save(any());
        verify(repository, never()).save(any(), any());
    }

    @Test
    void requiresAnAccountOwnerBeforeValidatingOrPersistingProviderCredentials() {
        assertThrows(IllegalArgumentException.class,
                () -> authorizationService.authorize(provider, TOKEN, null));

        verify(provider, never()).validateAccessToken(any());
        verify(repository, never()).save(any());
        verify(repository, never()).save(any(), any());
    }

    @Test
    void revalidatesAndRefreshesStoredExpiry() {
        ChannelAuthorization existing = authorization("old-access", "old-refresh");
        when(repository.findByProviderUserId("youtube", "external-user-1001")).thenReturn(Optional.of(existing));
        when(provider.validateAccessToken("old-access")).thenReturn(identity("external-user-1001"));

        ChannelAuthorization validated = authorizationService.validateOrRefresh(provider, existing);

        assertEquals(NOW.plusSeconds(3600), validated.expiresAt());
        assertEquals(NOW, validated.validatedAt());
        verify(repository).save(validated);
        verify(provider, never()).refreshAccessToken(any());
    }

    @Test
    void refreshesUnauthorizedAccessAndPersistsBothRotatedTokens() {
        ChannelAuthorization existing = authorization("old-access", "old-refresh");
        OAuthProviderException unauthorized = new OAuthProviderException("youtube", 401);
        OAuthTokenResponse rotated = new OAuthTokenResponse("new-access", "new-refresh", 3600, REQUIRED_SCOPES, "bearer");
        when(repository.findByProviderUserId("youtube", "external-user-1001")).thenReturn(Optional.of(existing));
        when(provider.validateAccessToken("old-access")).thenThrow(unauthorized);
        when(provider.isInvalidAccessTokenFailure(unauthorized)).thenReturn(true);
        when(provider.refreshAccessToken("old-refresh")).thenReturn(rotated);
        when(provider.validateAccessToken("new-access")).thenReturn(identity("external-user-1001"));

        ChannelAuthorization validated = authorizationService.validateOrRefresh(provider, existing);

        assertEquals("new-access", validated.accessToken());
        assertEquals("new-refresh", validated.refreshToken());
        assertEquals(ChannelAuthorization.Status.AUTHORIZED, validated.status());
        verify(repository).save(validated);
    }

    @Test
    void invalidRotatingRefreshTokenMarksOnlyThatAuthorizationForReauthorization() {
        ChannelAuthorization existing = authorization("old-access", "old-refresh");
        OAuthProviderException unauthorizedAccess = new OAuthProviderException("youtube", 401);
        OAuthProviderException invalidRefresh = new OAuthProviderException("youtube", 400);
        when(repository.findByProviderUserId("youtube", "external-user-1001")).thenReturn(Optional.of(existing));
        when(provider.validateAccessToken("old-access")).thenThrow(unauthorizedAccess);
        when(provider.isInvalidAccessTokenFailure(unauthorizedAccess)).thenReturn(true);
        when(provider.refreshAccessToken("old-refresh")).thenThrow(invalidRefresh);
        when(provider.isInvalidRefreshTokenFailure(invalidRefresh)).thenReturn(true);

        ChannelAuthorization result = authorizationService.validateOrRefresh(provider, existing);

        assertEquals(ChannelAuthorization.Status.NEEDS_REAUTHORIZATION, result.status());
        assertEquals(existing.accessToken(), result.accessToken());
        assertEquals(existing.refreshToken(), result.refreshToken());
        verify(repository).save(result);
    }

    @Test
    void doesNotInvalidateCredentialsOnTransientProviderFailure() {
        ChannelAuthorization existing = authorization("old-access", "old-refresh");
        OAuthProviderException temporaryFailure = new OAuthProviderException("youtube", 503);
        when(repository.findByProviderUserId("youtube", "external-user-1001")).thenReturn(Optional.of(existing));
        when(provider.validateAccessToken("old-access")).thenThrow(temporaryFailure);

        assertThrows(OAuthProviderException.class,
                () -> authorizationService.validateOrRefresh(provider, existing));

        verify(repository, never()).save(any());
        verify(provider, never()).refreshAccessToken(any());
    }

    @Test
    void concurrentValidationUsesRotatedStoredTokensInsteadOfRefreshingTwice() throws Exception {
        ChannelAuthorization existing = authorization("old-access", "old-refresh");
        AtomicReference<ChannelAuthorization> persisted = new AtomicReference<>(existing);
        when(repository.findByProviderUserId("youtube", "external-user-1001"))
                .thenAnswer(invocation -> Optional.of(persisted.get()));
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return null;
        }).when(repository).save(any(ChannelAuthorization.class));

        CountDownLatch firstValidationEntered = new CountDownLatch(1);
        CountDownLatch allowFirstValidationToFail = new CountDownLatch(1);
        OAuthProviderException unauthorized = new OAuthProviderException("youtube", 401);
        when(provider.validateAccessToken("old-access")).thenAnswer(invocation -> {
            firstValidationEntered.countDown();
            if (!allowFirstValidationToFail.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test synchronization timed out");
            }
            throw unauthorized;
        });
        when(provider.isInvalidAccessTokenFailure(unauthorized)).thenReturn(true);
        when(provider.refreshAccessToken("old-refresh")).thenReturn(
                new OAuthTokenResponse("new-access", "new-refresh", 3600, REQUIRED_SCOPES, "bearer"));
        when(provider.validateAccessToken("new-access")).thenReturn(identity("external-user-1001"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> authorizationService.validateOrRefresh(provider, existing));
            if (!firstValidationEntered.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("first validation did not start");
            }
            var second = executor.submit(() -> authorizationService.validateOrRefresh(provider, existing));
            allowFirstValidationToFail.countDown();

            assertEquals("new-refresh", first.get(5, TimeUnit.SECONDS).refreshToken());
            assertEquals("new-refresh", second.get(5, TimeUnit.SECONDS).refreshToken());
            verify(provider, times(1)).refreshAccessToken("old-refresh");
        } finally {
            executor.shutdownNow();
        }
    }

    private static OAuthIdentity identity(String userId) {
        return new OAuthIdentity("expected-client-id", userId, "streamer_login", REQUIRED_SCOPES, 3600);
    }

    private static ChannelAuthorization authorization(String accessToken, String refreshToken) {
        return new ChannelAuthorization(
                "youtube", "external-user-1001", "streamer_login", accessToken, refreshToken,
                REQUIRED_SCOPES, NOW.plusSeconds(1800), ChannelAuthorization.Status.AUTHORIZED, NOW, NOW);
    }
}
