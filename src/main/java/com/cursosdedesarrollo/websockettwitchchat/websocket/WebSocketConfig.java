package com.cursosdedesarrollo.websockettwitchchat.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Value("${chat.websocket.allowed-origins:http://localhost:8080,http://127.0.0.1:8080}")
    private String allowedOrigins = "http://localhost:8080,http://127.0.0.1:8080";



    private MyWebSocketHandler myWebSocketHandler;

    @Autowired
    public WebSocketConfig(MyWebSocketHandler myWebSocketHandler){
        this.myWebSocketHandler = myWebSocketHandler;
    }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myWebSocketHandler, "/websocket")
                .setAllowedOrigins(allowedOrigins.split(","));
    }

}

