package com.cursosdedesarrollo.websockettwitchchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class TestWebsocketTwitchChatApplication {

    public static void main(String[] args) {
        SpringApplication.from(WebsocketTwitchChatApplication::main).with(TestWebsocketTwitchChatApplication.class).run(args);
    }

}
