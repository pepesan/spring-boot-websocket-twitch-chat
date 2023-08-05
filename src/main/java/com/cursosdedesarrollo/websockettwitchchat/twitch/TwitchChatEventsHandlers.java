package com.cursosdedesarrollo.websockettwitchchat.twitch;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TwitchChatEventsHandlers {



    @Autowired
    private TwitchChatClient twitchChatClient;



    private static final Logger logger = LoggerFactory.getLogger(TwitchChatEventsHandlers.class);

}
