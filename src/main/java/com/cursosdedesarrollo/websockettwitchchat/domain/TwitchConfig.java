package com.cursosdedesarrollo.websockettwitchchat.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
@Entity
public class TwitchConfig {
    @Id
    @Value("${twitch.clientId}")
    private String clientId;

    @Value("${twitch.clientSecret}")
    private String clientSecret;

    @Value("${twitch.accessToken}")
    private String accessToken;

    @Value("${twitch.channel}")
    private String channel;

    @Value("${twitch.oauthToken}")
    private String oauthToken;

    @Value("${twitch.channelID}")
    private String channelId;

    @Value("${twitch.refreshToken}")
    private String refreshToken;

    @Value("${twitch.state}")
    private String state;

    @Value("${twitch.redirect_uri}")
    private String redirectUri;

}

