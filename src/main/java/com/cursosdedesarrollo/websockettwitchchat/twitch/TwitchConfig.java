package com.cursosdedesarrollo.websockettwitchchat.twitch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TwitchConfig {
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
    // Resto de la configuración de Twitch
}

