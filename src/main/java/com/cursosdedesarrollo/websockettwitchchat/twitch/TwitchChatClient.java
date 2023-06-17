package com.cursosdedesarrollo.websockettwitchchat.twitch;

import com.cursosdedesarrollo.websockettwitchchat.websocket.MyWebSocketHandler;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.helix.domain.UserList;
import com.github.twitch4j.helix.domain.Video;
import com.github.twitch4j.helix.domain.VideoList;
import com.netflix.hystrix.HystrixCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.api.domain.IDisposable;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class TwitchChatClient {

    private TwitchClient twitchClient;
    private static final Logger logger = LoggerFactory.getLogger(TwitchChatClient.class);

    @Autowired
    private TwitchChatEventsHandlers twitchChatEventsHandlers;

    @Autowired
    private MyWebSocketHandler myWebSocketHandler;
    IDisposable handlerOnMessage;
    private String channel;
    private String oauthToken;
    public TwitchChatClient(@Value("${twitch.clientId}") String clientId,
                            @Value("${twitch.clientSecret}") String clientSecret,
                            @Value("${twitch.accessToken}") String accessToken,
                            @Value("${twitch.oauthToken}") String oauthToken,
                            @Value("${twitch.channel}") String channel) {
        this.channel = channel;
        this.oauthToken = oauthToken;
        twitchClient = TwitchClientBuilder.builder()
                .withEnableHelix(true)
                .withEnableChat(true)
                .withEnableEventSocket(true)
                .withEnablePubSub(true)
                .withChatAccount(new OAuth2Credential("twitch", oauthToken))
                .build();
    }

    public void connectAndJoinChannel(String channelName) {
        twitchClient.getChat().joinChannel(channelName);
        registerEvents();
    }
    public String getChannelID(String channelName ){
        List<User> users = twitchClient.getHelix().getUsers(
                channel,
                Arrays.asList(),
                Collections.singletonList(channelName)).execute().getUsers();
        logger.info(users.get(0).toString());
        return users.get(0).toString();
    }
    /*
    public String getLastChannelVideoTitle(String channelName ){
        VideoList videoList = this.twitchClient.getHelix().getStreams(
                null,
                null,
                null,
                1,
                null,
                new ArrayList<>(),
                )
        if (videoList != null && videoList.getVideos() != null && !videoList.getVideos().isEmpty()) {
            Video ultimoVideo = videoList.getVideos().get(0);
            String tituloUltimoVideo = ultimoVideo.getTitle();
            System.out.println("Título del último video: " + tituloUltimoVideo);
        }
        return "Ni idea";
    }

     */

    public void registerEvents(){
        handlerOnMessage = twitchClient
                .getEventManager()
                .onEvent(
                        ChannelMessageEvent.class,
                        event -> {
                            logger.info("[" + event.getChannel().getName() + "]["+event.getPermissions().toString()+"] " + event.getUser().getName() + ": " + event.getMessage());
                            if(event.getMessage().startsWith("!")){
                                this.twitchChatEventsHandlers.executeCommands(event, this, channel, myWebSocketHandler);
                            }else {
                                logger.info("received message: {}", event.getMessage());
                                try {
                                    myWebSocketHandler.sendMessage(event);
                                } catch (IOException e) {
                                    logger.error("fallo al mandar mensaje al webssocket");
                                    throw new RuntimeException(e);
                                }
                                // this.sendMessage(channel,"Estamos grabando un curso para Youtube. Mientras tanto no podemos atender al chat. Cuando terminemos podemos atender tus preguntas.");
                            }

                        }
                );
    }

    public void sendMessage(String channelName, String message) {
        twitchClient.getChat().sendMessage(channelName, message);
    }

    public void disconnect() {
        // cancel handler (don't call the method for new events of the required type anymore)
        handlerOnMessage.dispose();
        twitchClient.close();
    }
}
