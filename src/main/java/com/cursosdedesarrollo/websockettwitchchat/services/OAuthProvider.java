package com.cursosdedesarrollo.websockettwitchchat.services;

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
}
