package com.cursosdedesarrollo.websockettwitchchat.twitch;


import com.cursosdedesarrollo.websockettwitchchat.jsonconfig.Command;
import com.cursosdedesarrollo.websockettwitchchat.jsonconfig.JsonConfigService;
import com.cursosdedesarrollo.websockettwitchchat.websocket.MyWebSocketHandler;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
public class TwitchChatEventsHandlers {



    @Autowired
    private TwitchChatClient twitchChatClient;



    private static final Logger logger = LoggerFactory.getLogger(TwitchChatEventsHandlers.class);

}
