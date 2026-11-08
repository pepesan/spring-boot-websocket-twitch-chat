package com.cursosdedesarrollo.websockettwitchchat.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OAuthIdentity(
        @JsonProperty("client_id") String clientId,
        @JsonProperty("user_id") String userId,
        @JsonProperty("login") String login,
        @JsonProperty("scopes") List<String> scopes,
        @JsonProperty("expires_in") long expiresIn) {
}
