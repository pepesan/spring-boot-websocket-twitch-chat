package com.cursosdedesarrollo.websockettwitchchat.services;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthProviderRegistryTest {
    @Test
    void selectsTheProviderByStableIdentifier() {
        OAuthProvider twitch = provider("twitch");
        OAuthProvider youtube = provider("youtube");
        OAuthProviderRegistry registry = new OAuthProviderRegistry(List.of(twitch, youtube));

        assertSame(twitch, registry.require("twitch"));
        assertSame(youtube, registry.require("youtube"));
    }

    @Test
    void rejectsUnknownAndDuplicateProviders() {
        OAuthProvider twitch = provider("twitch");
        OAuthProviderRegistry registry = new OAuthProviderRegistry(List.of(twitch));

        assertThrows(IllegalArgumentException.class, () -> registry.require("unknown"));
        assertThrows(IllegalArgumentException.class,
                () -> new OAuthProviderRegistry(List.of(twitch, provider("twitch"))));
    }

    private static OAuthProvider provider(String providerId) {
        OAuthProvider provider = mock(OAuthProvider.class);
        when(provider.providerId()).thenReturn(providerId);
        return provider;
    }
}
