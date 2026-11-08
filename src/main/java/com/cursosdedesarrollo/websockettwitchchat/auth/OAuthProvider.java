package com.cursosdedesarrollo.websockettwitchchat.auth;

import java.net.URI;
import java.util.List;

/** Provider-specific implementation of the common authorization-code and token lifecycle. */
public interface OAuthProvider {
    String providerId();

    String clientId();

    List<String> requiredScopes();

    URI authorizationUri(String state);

    OAuthTokenResponse exchangeAuthorizationCode(String authorizationCode);

    OAuthTokenResponse refreshAccessToken(String refreshToken);

    OAuthIdentity validateAccessToken(String accessToken);

    default boolean isInvalidAccessTokenFailure(OAuthProviderException failure) {
        return providerId().equals(failure.providerId()) && failure.statusCode() == 401;
    }

    default boolean isInvalidRefreshTokenFailure(OAuthProviderException failure) {
        return providerId().equals(failure.providerId())
                && (failure.statusCode() == 400 || failure.statusCode() == 401);
    }
}
