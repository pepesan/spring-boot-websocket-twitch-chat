package com.cursosdedesarrollo.websockettwitchchat.services;

import com.cursosdedesarrollo.websockettwitchchat.domain.TwitchConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
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

import java.util.List;

@Service
public class TwitchAuthService {
    private static final String TOKEN_ENDPOINT = "https://id.twitch.tv/oauth2/token";
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

    public TokenResponse exchangeAuthorizationCode(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", twitchConfig.getClientId());
        form.add("client_secret", twitchConfig.getClientSecret());
        form.add("code", authorizationCode);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", twitchConfig.getRedirectUri());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<TokenResponse> response = restTemplate.exchange(
                    TOKEN_ENDPOINT,
                    HttpMethod.POST,
                    new HttpEntity<>(form, headers),
                    TokenResponse.class
            );
            TokenResponse token = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful()
                    || token == null
                    || token.accessToken() == null
                    || token.accessToken().isBlank()) {
                throw new IllegalStateException("Twitch returned an incomplete token response");
            }
            return token;
        } catch (HttpStatusCodeException exception) {
            logger.error("Twitch authorization-code exchange failed with HTTP status {}", exception.getStatusCode());
            throw new IllegalStateException("Twitch authorization-code exchange failed", exception);
        }
    }

    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("scope") List<String> scopes,
            @JsonProperty("token_type") String tokenType) {
    }
}
