package com.cursosdedesarrollo.websockettwitchchat.services;

import com.cursosdedesarrollo.websockettwitchchat.domain.TwitchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class TwitchAuthService implements OAuthProvider {
    private static final String AUTHORIZE_ENDPOINT = "https://id.twitch.tv/oauth2/authorize";
    private static final String TOKEN_ENDPOINT = "https://id.twitch.tv/oauth2/token";
    private static final String VALIDATE_ENDPOINT = "https://id.twitch.tv/oauth2/validate";
    private static final List<String> REQUIRED_SCOPES = List.of(
            "chat:read", "chat:edit", "moderator:read:followers", "bits:read", "channel:read:subscriptions");
    private static final Logger logger = LoggerFactory.getLogger(TwitchAuthService.class);

    private final TwitchConfig twitchConfig;
    private final RestTemplate restTemplate;

    @Autowired
    public TwitchAuthService(TwitchConfig twitchConfig) {
        this(twitchConfig, new RestTemplate());
    }

    TwitchAuthService(TwitchConfig twitchConfig, RestTemplate restTemplate) {
        this.twitchConfig = twitchConfig;
        this.restTemplate = restTemplate;
    }

    @Override
    public String providerId() {
        return "twitch";
    }

    @Override
    public String clientId() {
        return twitchConfig.getClientId();
    }

    @Override
    public List<String> requiredScopes() {
        return REQUIRED_SCOPES;
    }

    @Override
    public URI authorizationUri(String state) {
        return org.springframework.web.util.UriComponentsBuilder.fromUriString(AUTHORIZE_ENDPOINT)
                .queryParam("client_id", clientId())
                .queryParam("redirect_uri", twitchConfig.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", requiredScopes()))
                .queryParam("state", state)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
    }

    @Override
    public OAuthTokenResponse exchangeAuthorizationCode(String authorizationCode) {
        MultiValueMap<String, String> form = baseTokenForm();
        form.add("code", authorizationCode);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", twitchConfig.getRedirectUri());
        return requestToken(form, "authorization-code exchange");
    }

    @Override
    public OAuthTokenResponse refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }
        MultiValueMap<String, String> form = baseTokenForm();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        return requestToken(form, "token refresh");
    }

    @Override
    public OAuthIdentity validateAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token is required");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "OAuth " + accessToken);
        try {
            ResponseEntity<OAuthIdentity> response = restTemplate.exchange(
                    VALIDATE_ENDPOINT, HttpMethod.GET, new HttpEntity<>(headers), OAuthIdentity.class);
            OAuthIdentity validation = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful()
                    || validation == null
                    || validation.clientId() == null
                    || validation.userId() == null
                    || validation.userId().isBlank()
                    || validation.login() == null
                    || validation.login().isBlank()
                    || validation.expiresIn() <= 0) {
                throw new IllegalStateException("Twitch returned incomplete token validation data");
            }
            return validation;
        } catch (HttpStatusCodeException exception) {
            logHttpFailure("token validation", exception);
            throw new TwitchAuthException(exception.getStatusCode().value());
        }
    }

    private MultiValueMap<String, String> baseTokenForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", twitchConfig.getClientId());
        form.add("client_secret", twitchConfig.getClientSecret());
        return form;
    }

    private OAuthTokenResponse requestToken(MultiValueMap<String, String> form, String operation) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            ResponseEntity<OAuthTokenResponse> response = restTemplate.exchange(
                    TOKEN_ENDPOINT, HttpMethod.POST, new HttpEntity<>(form, headers), OAuthTokenResponse.class);
            OAuthTokenResponse token = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful()
                    || token == null
                    || token.accessToken() == null
                    || token.accessToken().isBlank()
                    || token.refreshToken() == null
                    || token.refreshToken().isBlank()
                    || token.expiresIn() <= 0) {
                throw new IllegalStateException("Twitch returned an incomplete token response");
            }
            return token;
        } catch (HttpStatusCodeException exception) {
            logHttpFailure(operation, exception);
            throw new TwitchAuthException(exception.getStatusCode().value());
        }
    }

    private void logHttpFailure(String operation, HttpStatusCodeException exception) {
        logger.error("Twitch {} failed with HTTP status {}", operation, exception.getStatusCode());
    }

    public static final class TwitchAuthException extends RuntimeException {
        private final int statusCode;

        public TwitchAuthException(int statusCode) {
            super("Twitch authentication request failed with HTTP status " + statusCode);
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }

}
