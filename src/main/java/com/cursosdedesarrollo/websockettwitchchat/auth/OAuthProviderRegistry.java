package com.cursosdedesarrollo.websockettwitchchat.auth;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OAuthProviderRegistry {
    private final Map<String, OAuthProvider> providers;

    public OAuthProviderRegistry(List<OAuthProvider> providers) {
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                OAuthProvider::providerId,
                Function.identity(),
                (first, duplicate) -> {
                    throw new IllegalArgumentException("Duplicate OAuth provider ID: " + first.providerId());
                }));
    }

    public OAuthProvider require(String providerId) {
        OAuthProvider provider = providers.get(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported OAuth provider: " + providerId);
        }
        return provider;
    }
}
