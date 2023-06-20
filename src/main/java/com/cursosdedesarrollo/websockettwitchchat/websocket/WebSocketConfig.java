package com.cursosdedesarrollo.websockettwitchchat.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {


    private MyWebSocketHandler myWebSocketHandler;

    @Autowired
    public WebSocketConfig(MyWebSocketHandler myWebSocketHandler){
        this.myWebSocketHandler = myWebSocketHandler;
    }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myWebSocketHandler, "/websocket").setAllowedOrigins("*");
    }

}

