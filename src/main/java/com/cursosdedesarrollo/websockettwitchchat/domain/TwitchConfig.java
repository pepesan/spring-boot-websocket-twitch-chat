package com.cursosdedesarrollo.websockettwitchchat.domain;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class TwitchConfig {
    @Value("${twitch.clientId}")
    private String clientId;

    @Value("${twitch.clientSecret}")
    private String clientSecret;

    @Value("${twitch.redirect_uri}")
    private String redirectUri;
}
