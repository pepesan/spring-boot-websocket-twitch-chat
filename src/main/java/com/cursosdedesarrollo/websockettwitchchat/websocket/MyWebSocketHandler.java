package com.cursosdedesarrollo.websockettwitchchat.websocket;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.events.domain.EventUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
public class MyWebSocketHandler extends TextWebSocketHandler {
    private final Logger logger = LoggerFactory.getLogger(MyWebSocketHandler.class);
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private WebSocketSession session;
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.session = session;
        /*
        executorService.scheduleAtFixedRate(() -> {
            String randomMessage = generateRandomMessage();
            logger.info("Sending message to client: {}", randomMessage);
            try {

                session.sendMessage(new TextMessage(randomMessage));
            } catch (IOException e) {
                logger.error("Error sending message to client", e);
            }
        }, 0, 1, TimeUnit.SECONDS);

         */
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("cerrando conexión a websocket client");
        // executorService.shutdown();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Manejar el mensaje recibido desde el cliente
        String receivedMessage = message.getPayload();
        logger.info("Received message from client: {}", receivedMessage);
    }

    public void sendMessage(ChannelMessageEvent event) throws IOException {
        // logger.info("[" + event.getChannel().getName() + "]["+event.getPermissions().toString()+"] " + event.getUser().getName() + ": " + event.getMessage());
        logger.info("mandando mensaje a websocket");
        EventUser user = event.getUser();
        boolean firstMessage = event.isDesignatedFirstMessage();
        this.session.sendMessage(new TextMessage(
                "{" +
                        "\"username\": \""+user.getName()+ "\", "+
                        "\"userId\": \""+user.getId()+ "\", "+
                        "\"firstMessage\": "+firstMessage+ ", "+
                        "\"message\": \""+event.getMessage()+ "\""+
                        "}"
        ));
    }

    private String generateRandomMessage() {
        // Generar un mensaje aleatorio
        Random random = new Random();
        int randomNumber = random.nextInt(100);
        return "Random number: " + randomNumber;
    }
}
