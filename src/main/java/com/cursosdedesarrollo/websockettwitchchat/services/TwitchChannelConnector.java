package com.cursosdedesarrollo.websockettwitchchat.services;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.twitch.TwitchChatClient;
import com.cursosdedesarrollo.websockettwitchchat.twitch.TwitchHelixClient;
import org.springframework.stereotype.Component;

@Component
public class TwitchChannelConnector implements ChannelConnector {
    private final TwitchChatClient chatClient;
    private final TwitchHelixClient helixClient;

    public TwitchChannelConnector(TwitchChatClient chatClient, TwitchHelixClient helixClient) {
        this.chatClient = chatClient;
        this.helixClient = helixClient;
    }

    @Override
    public String providerId() {
        return "twitch";
    }

    @Override
    public void connect(ChannelAuthorization authorization) {
        if (!providerId().equals(authorization.providerId())) {
            throw new IllegalArgumentException("Authorization belongs to a different channel provider");
        }
        helixClient.connect();
        chatClient.connect(authorization);
        chatClient.connectAndJoinChannel();
    }
}
