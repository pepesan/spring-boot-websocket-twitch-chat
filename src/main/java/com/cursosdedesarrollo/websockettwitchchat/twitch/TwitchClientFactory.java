package com.cursosdedesarrollo.websockettwitchchat.twitch;

import com.cursosdedesarrollo.websockettwitchchat.domain.TwitchConfig;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import org.springframework.stereotype.Component;

@Component
public class TwitchClientFactory {
    private final TwitchConfig twitchConfig;

    public TwitchClientFactory(TwitchConfig twitchConfig) {
        this.twitchConfig = twitchConfig;
    }

    public TwitchClient create(OAuth2Credential credential) {
        return TwitchClientBuilder.builder()
                .withClientId(twitchConfig.getClientId())
                .withClientSecret(twitchConfig.getClientSecret())
                .withEnableHelix(true)
                .withEnableChat(true)
                .withEnableEventSocket(true)
                .withChatAccount(credential)
                .withDefaultAuthToken(credential)
                .withDefaultEventHandler(SimpleEventHandler.class)
                .build();
    }
}
