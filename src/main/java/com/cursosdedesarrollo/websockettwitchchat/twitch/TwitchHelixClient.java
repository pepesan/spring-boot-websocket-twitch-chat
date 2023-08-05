package com.cursosdedesarrollo.websockettwitchchat.twitch;

import com.cursosdedesarrollo.websockettwitchchat.domain.TwitchConfig;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.TwitchHelixBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TwitchHelixClient {

    private static final Logger logger = LoggerFactory.getLogger(TwitchHelixClient.class);


    public TwitchHelix client;

    @Autowired
    private TwitchConfig twitchConfig;



    public void connect(){
        client = TwitchHelixBuilder.builder()
                .withClientId(this.twitchConfig.getClientId())
                .withClientSecret(this.twitchConfig.getClientSecret())
                .build();
        logger.info("Bot Helix conectando");
    }

}
