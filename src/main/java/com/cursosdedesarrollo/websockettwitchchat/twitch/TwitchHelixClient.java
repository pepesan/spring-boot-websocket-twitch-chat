package com.cursosdedesarrollo.websockettwitchchat.twitch;

import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.TwitchHelixBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TwitchHelixClient {

    public TwitchHelix client;

    TwitchHelixClient(@Value("${twitch.clientId}") String clientId,
                      @Value("${twitch.clientSecret}") String clientSecret,
                      @Value("${twitch.accessToken}") String accessToken,
                      @Value("${twitch.oauthToken}") String oauthToken,
                      @Value("${twitch.channel}") String channel){
        client = TwitchHelixBuilder.builder()
                .withClientId(clientId)
                .withClientSecret(clientSecret)
                .build();
    }
}
