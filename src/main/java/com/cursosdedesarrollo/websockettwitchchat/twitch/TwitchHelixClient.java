package com.cursosdedesarrollo.websockettwitchchat.twitch;

import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.TwitchHelixBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TwitchHelixClient {

    public TwitchHelix client;

    public String channelName;

    public String accessToken;

    public String oauthToken;


    TwitchHelixClient(@Value("${twitch.clientId}") String clientId,
                      @Value("${twitch.clientSecret}") String clientSecret,
                      @Value("${twitch.accessToken}") String accessToken,
                      @Value("${twitch.oauthToken}") String oauthToken,
                      @Value("${twitch.channel}") String channel){
        client = TwitchHelixBuilder.builder()
                .withClientId(clientId)
                .withClientSecret(clientSecret)
                .build();

        this.channelName = channel;
        this.accessToken = accessToken;
        this.oauthToken = oauthToken;
    }
}
