package com.cursosdedesarrollo.websockettwitchchat.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OAuthTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("scope") List<String> scopes,
        @JsonProperty("token_type") String tokenType) {
}
