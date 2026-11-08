package com.cursosdedesarrollo.websockettwitchchat.auth;

import com.cursosdedesarrollo.websockettwitchchat.channels.ChannelConnector;
import com.cursosdedesarrollo.websockettwitchchat.channels.ChannelConnectorRegistry;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.repositories.SQLiteChannelAuthorizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChannelTokenLifecycleServiceTest {
    private SQLiteChannelAuthorizationRepository repository;
    private OAuthProvider provider;
    private ChannelConnector connector;
    private ChannelAuthorizationService authorizationService;
    private ChannelTokenLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        repository = mock(SQLiteChannelAuthorizationRepository.class);
        provider = mock(OAuthProvider.class);
        when(provider.providerId()).thenReturn("twitch");
        connector = mock(ChannelConnector.class);
        when(connector.providerId()).thenReturn("twitch");
        authorizationService = mock(ChannelAuthorizationService.class);
        lifecycleService = new ChannelTokenLifecycleService(
                repository,
                new OAuthProviderRegistry(List.of(provider)),
                new ChannelConnectorRegistry(List.of(connector)),
                authorizationService);
    }

    @Test
    void disconnectsOnlyAuthorizationMarkedForReauthorization() {
        ChannelAuthorization authorized = authorization(ChannelAuthorization.Status.AUTHORIZED, "access", "refresh");
        ChannelAuthorization reauthorizationRequired = authorization(
                ChannelAuthorization.Status.NEEDS_REAUTHORIZATION, "access", "refresh");
        when(repository.findAll()).thenReturn(List.of(authorized));
        when(connector.isConnected(authorized)).thenReturn(true);
        when(authorizationService.validateOrRefresh(provider, authorized)).thenReturn(reauthorizationRequired);

        lifecycleService.validateStoredAuthorizations();

        verify(connector).disconnect(authorized);
        verify(connector, never()).connect(reauthorizationRequired);
    }

    @Test
    void reconnectsAnActiveChannelAfterBothTokensRotate() {
        ChannelAuthorization previous = authorization(ChannelAuthorization.Status.AUTHORIZED, "old-access", "old-refresh");
        ChannelAuthorization updated = authorization(ChannelAuthorization.Status.AUTHORIZED, "new-access", "new-refresh");
        when(repository.findAll()).thenReturn(List.of(previous));
        when(connector.isConnected(previous)).thenReturn(true);
        when(authorizationService.validateOrRefresh(provider, previous)).thenReturn(updated);

        lifecycleService.validateStoredAuthorizations();

        verify(connector).disconnect(previous);
        verify(connector).connect(updated);
    }


    @Test
    void restoresAuthorizedChannelConnectionAfterStartupValidation() {
        ChannelAuthorization authorized = authorization(ChannelAuthorization.Status.AUTHORIZED, "access", "refresh");
        when(repository.findAll()).thenReturn(List.of(authorized));
        when(connector.isConnected(authorized)).thenReturn(false);
        when(authorizationService.validateOrRefresh(provider, authorized)).thenReturn(authorized);

        lifecycleService.validateStoredAuthorizations();

        verify(connector).connect(authorized);
    }
    @Test
    void leavesAnActiveConnectionAloneWhenValidationFailsTransiently() {
        ChannelAuthorization existing = authorization(ChannelAuthorization.Status.AUTHORIZED, "access", "refresh");
        when(repository.findAll()).thenReturn(List.of(existing));
        when(connector.isConnected(existing)).thenReturn(true);
        when(authorizationService.validateOrRefresh(provider, existing))
                .thenThrow(new OAuthProviderException("twitch", 503));

        lifecycleService.validateStoredAuthorizations();

        verify(connector, never()).disconnect(existing);
        verify(connector, never()).connect(existing);
    }

    private static ChannelAuthorization authorization(
            ChannelAuthorization.Status status, String accessToken, String refreshToken) {
        Instant now = Instant.parse("2026-11-08T12:00:00Z");
        return new ChannelAuthorization(
                "twitch", "1001", "streamer", accessToken, refreshToken, List.of("chat:read"),
                now.plusSeconds(3600), status, now, now);
    }
}
