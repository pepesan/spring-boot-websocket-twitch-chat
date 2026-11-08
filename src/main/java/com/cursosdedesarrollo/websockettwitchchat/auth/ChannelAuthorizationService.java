package com.cursosdedesarrollo.websockettwitchchat.auth;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.repositories.SQLiteChannelAuthorizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChannelAuthorizationService {
    private final SQLiteChannelAuthorizationRepository authorizationRepository;
    private final Clock clock;
    private final Map<AuthorizationKey, Object> authorizationLocks = new ConcurrentHashMap<>();

    @Autowired
    public ChannelAuthorizationService(SQLiteChannelAuthorizationRepository authorizationRepository) {
        this(authorizationRepository, Clock.systemUTC());
    }

    ChannelAuthorizationService(SQLiteChannelAuthorizationRepository authorizationRepository, Clock clock) {
        this.authorizationRepository = authorizationRepository;
        this.clock = clock;
    }

    public ChannelAuthorization authorize(
            OAuthProvider provider, OAuthTokenResponse token, Integer creatorAccountId) {
        if (creatorAccountId == null || creatorAccountId <= 0) {
            throw new IllegalArgumentException("A verified creator account is required");
        }
        requireCompleteToken(token);
        OAuthIdentity identity = provider.validateAccessToken(token.accessToken());
        if (identity == null || identity.userId() == null || identity.userId().isBlank()) {
            throw new InvalidAuthorizationException("OAuth provider returned an incomplete identity");
        }
        synchronized (lockFor(provider.providerId(), identity.userId())) {
            return saveValidated(provider, token.accessToken(), token.refreshToken(), identity, null, creatorAccountId);
        }
    }
    public ChannelAuthorization validateOrRefresh(OAuthProvider provider, ChannelAuthorization authorization) {
        if (!provider.providerId().equals(authorization.providerId())) {
            throw new IllegalArgumentException("Authorization belongs to a different OAuth provider");
        }
        synchronized (lockFor(authorization.providerId(), authorization.providerUserId())) {
            ChannelAuthorization current = authorizationRepository
                    .findByProviderUserId(authorization.providerId(), authorization.providerUserId())
                    .orElse(authorization);
            if (current.status() != ChannelAuthorization.Status.AUTHORIZED) {
                return current;
            }
            return validateCurrent(provider, current);
        }
    }

    private ChannelAuthorization validateCurrent(OAuthProvider provider, ChannelAuthorization authorization) {
        try {
            OAuthIdentity identity = provider.validateAccessToken(authorization.accessToken());
            return saveValidated(
                    provider, authorization.accessToken(), authorization.refreshToken(), identity,
                    authorization.providerUserId(), null);
        } catch (OAuthProviderException exception) {
            if (!provider.isInvalidAccessTokenFailure(exception)) {
                throw exception;
            }
        } catch (InvalidAuthorizationException exception) {
            return markNeedsReauthorization(authorization);
        }

        try {
            OAuthTokenResponse refreshed = provider.refreshAccessToken(authorization.refreshToken());
            requireCompleteToken(refreshed);
            OAuthIdentity identity = provider.validateAccessToken(refreshed.accessToken());
            return saveValidated(
                    provider, refreshed.accessToken(), refreshed.refreshToken(), identity,
                    authorization.providerUserId(), null);
        } catch (OAuthProviderException exception) {
            if (provider.isInvalidRefreshTokenFailure(exception)) {
                return markNeedsReauthorization(authorization);
            }
            throw exception;
        } catch (IllegalArgumentException exception) {
            return markNeedsReauthorization(authorization);
        }
    }

    private ChannelAuthorization saveValidated(
            OAuthProvider provider,
            String accessToken,
            String refreshToken,
            OAuthIdentity identity,
            String expectedProviderUserId,
            Integer creatorAccountId) {
        if (identity == null
                || identity.clientId() == null
                || !provider.clientId().equals(identity.clientId())
                || identity.userId() == null
                || identity.userId().isBlank()
                || identity.login() == null
                || identity.login().isBlank()
                || identity.expiresIn() <= 0) {
            throw new InvalidAuthorizationException("OAuth identity is incomplete or belongs to another application");
        }
        if (expectedProviderUserId != null && !expectedProviderUserId.equals(identity.userId())) {
            throw new InvalidAuthorizationException("OAuth identity changed during token validation");
        }
        List<String> scopes = identity.scopes() == null ? List.of() : List.copyOf(identity.scopes());
        if (!scopes.containsAll(provider.requiredScopes())) {
            throw new InvalidAuthorizationException("OAuth authorization is missing required scopes");
        }

        Instant now = clock.instant();
        ChannelAuthorization validated = new ChannelAuthorization(
                provider.providerId(),
                identity.userId(),
                identity.login(),
                accessToken,
                refreshToken,
                scopes,
                now.plusSeconds(identity.expiresIn()),
                ChannelAuthorization.Status.AUTHORIZED,
                now,
                now);
        if (creatorAccountId == null) {
            authorizationRepository.save(validated);
        } else {
            authorizationRepository.save(validated, creatorAccountId);
        }
        return validated;
    }

    private ChannelAuthorization markNeedsReauthorization(ChannelAuthorization authorization) {
        ChannelAuthorization invalid = new ChannelAuthorization(
                authorization.providerId(),
                authorization.providerUserId(),
                authorization.login(),
                authorization.accessToken(),
                authorization.refreshToken(),
                authorization.scopes(),
                authorization.expiresAt(),
                ChannelAuthorization.Status.NEEDS_REAUTHORIZATION,
                authorization.validatedAt(),
                clock.instant());
        authorizationRepository.save(invalid);
        return invalid;
    }

    private Object lockFor(String providerId, String providerUserId) {
        return authorizationLocks.computeIfAbsent(new AuthorizationKey(providerId, providerUserId), key -> new Object());
    }

    private static void requireCompleteToken(OAuthTokenResponse token) {
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()
                || token.refreshToken() == null || token.refreshToken().isBlank()
                || token.expiresIn() <= 0) {
            throw new IllegalArgumentException("OAuth provider returned incomplete authorization tokens");
        }
    }

    private record AuthorizationKey(String providerId, String providerUserId) {
    }

    private static final class InvalidAuthorizationException extends IllegalArgumentException {
        private InvalidAuthorizationException(String message) {
            super(message);
        }
    }
}
