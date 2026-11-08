package com.cursosdedesarrollo.websockettwitchchat.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.events.domain.EventUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MyWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(MyWebSocketHandler.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(session, 10_000, 1_048_576);
        sessions.put(session.getId(), safeSession);
        logger.debug("WebSocket client {} connected", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        logger.debug("WebSocket client {} disconnected", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        session.close(CloseStatus.NOT_ACCEPTABLE);
    }

    public void sendMessage(ChannelMessageEvent event) throws IOException {
        if (sessions.isEmpty()) {
            return;
        }

        EventUser user = event.getUser();
        String payload;
        try {
            payload = JSON.writeValueAsString(new ChatMessage(
                    user.getName(),
                    String.valueOf(user.getId()),
                    event.isDesignatedFirstMessage(),
                    event.getMessage()));
        } catch (JsonProcessingException exception) {
            logger.warn("Could not serialize Twitch chat message ({})", exception.getClass().getSimpleName());
            return;
        }

        TextMessage outgoing = new TextMessage(payload);
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            WebSocketSession session = entry.getValue();
            if (!session.isOpen()) {
                sessions.remove(entry.getKey(), session);
                continue;
            }
            try {
                session.sendMessage(outgoing);
            } catch (IOException exception) {
                sessions.remove(entry.getKey(), session);
                logger.debug("Removing WebSocket client {} after send failure ({})",
                        entry.getKey(), exception.getClass().getSimpleName());
            }
        }
    }

    private record ChatMessage(String username, String userId, boolean firstMessage, String message) {
    }
}
