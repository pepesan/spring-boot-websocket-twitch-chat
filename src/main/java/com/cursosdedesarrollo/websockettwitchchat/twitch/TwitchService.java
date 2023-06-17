package com.cursosdedesarrollo.websockettwitchchat.twitch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwitchService {

    private TwitchChatClient twitchChatClient;

    @Autowired
    TwitchService(TwitchChatClient twitchChatClient, @Value("${twitch.channel}") String channel){
        twitchChatClient.connectAndJoinChannel(channel);
        twitchChatClient.sendMessage(channel,"Bot activado!");
    }
}
