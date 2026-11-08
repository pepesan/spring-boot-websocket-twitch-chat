package com.cursosdedesarrollo.websockettwitchchat.services;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.repositories.SQLiteChannelAuthorizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class ChannelAuthorizationService {
    private final SQLiteChannelAuthorizationRepository authorizationRepository;
    private final Clock clock;

    @Autowired
    public ChannelAuthorizationService(SQLiteChannelAuthorizationRepository authorizationRepository) {
        this(authorizationRepository, Clock.systemUTC());
    }

    ChannelAuthorizationService(SQLiteChannelAuthorizationRepository authorizationRepository, Clock clock) {
        this.authorizationRepository = authorizationRepository;
        this.clock = clock;
    }

    public ChannelAuthorization authorize(OAuthProvider provider, OAuthTokenResponse token) {
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()
                || token.refreshToken() == null || token.refreshToken().isBlank()) {
            throw new IllegalArgumentException("OAuth provider returned incomplete authorization tokens");
        }

        OAuthIdentity identity = provider.validateAccessToken(token.accessToken());
        if (!provider.clientId().equals(identity.clientId())) {
            throw new IllegalArgumentException("Validated token belongs to a different OAuth application");
        }
        List<String> scopes = identity.scopes() == null ? List.of() : List.copyOf(identity.scopes());
        if (!scopes.containsAll(provider.requiredScopes())) {
            throw new IllegalArgumentException("OAuth authorization is missing required scopes");
        }

        Instant now = clock.instant();
        ChannelAuthorization authorization = new ChannelAuthorization(
                provider.providerId(),
                identity.userId(),
                identity.login(),
                token.accessToken(),
                token.refreshToken(),
                scopes,
                now.plusSeconds(identity.expiresIn()),
                ChannelAuthorization.Status.AUTHORIZED,
                now,
                now);
        authorizationRepository.save(authorization);
        return authorization;
    }
}
