package com.cursosdedesarrollo.websockettwitchchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableScheduling
public class WebsocketTwitchChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebsocketTwitchChatApplication.class, args);
    }

}
