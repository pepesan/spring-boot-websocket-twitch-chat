package com.cursosdedesarrollo.websockettwitchchat.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.events.domain.EventUser;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class MyWebSocketHandlerTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void broadcastsEscapedChatJsonToEveryConnectedClient() throws Exception {
        MyWebSocketHandler handler = new MyWebSocketHandler();
        WebSocketSession dashboard = openSession("dashboard");
        WebSocketSession overlay = openSession("overlay");
        handler.afterConnectionEstablished(dashboard);
        handler.afterConnectionEstablished(overlay);

        EventUser user = mock(EventUser.class);
        when(user.getName()).thenReturn("viewer \"one\"");
        when(user.getId()).thenReturn("user-42");
        ChannelMessageEvent event = mock(ChannelMessageEvent.class);
        when(event.getUser()).thenReturn(user);
        when(event.isDesignatedFirstMessage()).thenReturn(true);
        when(event.getMessage()).thenReturn("hello \"chat\"\nsecond line");

        handler.sendMessage(event);

        JsonNode dashboardJson = sentJson(dashboard);
        JsonNode overlayJson = sentJson(overlay);
        for (JsonNode json : new JsonNode[]{dashboardJson, overlayJson}) {
            assertEquals("viewer \"one\"", json.get("username").asText());
            assertEquals("user-42", json.get("userId").asText());
            assertTrue(json.get("firstMessage").asBoolean());
            assertEquals("hello \"chat\"\nsecond line", json.get("message").asText());
        }
    }

    private WebSocketSession openSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private JsonNode sentJson(WebSocketSession session) throws Exception {
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        return json.readTree(captor.getValue().getPayload());
    }
}
