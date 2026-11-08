package com.cursosdedesarrollo.websockettwitchchat.twitch;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.domain.Command;
import com.cursosdedesarrollo.websockettwitchchat.domain.Configuracion;
import com.cursosdedesarrollo.websockettwitchchat.commands.JsonConfigService;
import com.cursosdedesarrollo.websockettwitchchat.twitch.events.TwitchEventSubService;
import com.cursosdedesarrollo.websockettwitchchat.websocket.MyWebSocketHandler;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TwitchChatClientTest {

    @Test
    void disconnectingOneChannelKeepsOtherChannelConnected() {
        TwitchClient firstClient = mock(TwitchClient.class, RETURNS_DEEP_STUBS);
        TwitchClient secondClient = mock(TwitchClient.class, RETURNS_DEEP_STUBS);
        TwitchClientFactory clientFactory = mock(TwitchClientFactory.class);
        when(clientFactory.create(any())).thenReturn(firstClient, secondClient);

        TwitchChatClient chatClient = new TwitchChatClient(
                mock(JsonConfigService.class),
                mock(TwitchHelixClient.class),
                mock(MyWebSocketHandler.class),
                mock(TwitchEventSubService.class),
                clientFactory);

        chatClient.connect(authorization("channel-1", "first"));
        chatClient.connect(authorization("channel-2", "second"));
        assertTrue(chatClient.isConnected("channel-1"));
        assertTrue(chatClient.isConnected("channel-2"));

        chatClient.disconnect("channel-1");

        assertFalse(chatClient.isConnected("channel-1"));
        assertTrue(chatClient.isConnected("channel-2"));
        verify(firstClient).close();
    }

    @Test
    void dispatchesConfiguredPrefixCommandToItsSourceChannel() {
        TwitchClient firstClient = mock(TwitchClient.class, RETURNS_DEEP_STUBS);
        TwitchClient secondClient = mock(TwitchClient.class, RETURNS_DEEP_STUBS);
        TwitchClientFactory clientFactory = mock(TwitchClientFactory.class);
        when(clientFactory.create(any())).thenReturn(firstClient, secondClient);

        JsonConfigService configService = mock(JsonConfigService.class);
        Configuracion configuration = mock(Configuracion.class);
        Command command = mock(Command.class);
        when(configService.getConfiguracion()).thenReturn(configuration);
        when(configuration.getCommandSymbol()).thenReturn("?");
        when(configuration.buscarComando("help")).thenReturn(command);
        when(command.getOwnerOnly()).thenReturn(false);
        when(command.getOnlyMods()).thenReturn(false);
        when(command.getOutputType()).thenReturn("normal");
        when(command.getOutputText()).thenReturn("response");

        TwitchChatClient chatClient = new TwitchChatClient(
                configService,
                mock(TwitchHelixClient.class),
                mock(MyWebSocketHandler.class),
                mock(TwitchEventSubService.class),
                clientFactory);
        chatClient.connect(authorization("channel-1", "first"));
        chatClient.connect(authorization("channel-2", "second"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.function.Consumer<ChannelMessageEvent>> handlerCaptor =
                (ArgumentCaptor<java.util.function.Consumer<ChannelMessageEvent>>) (ArgumentCaptor<?>)
                        ArgumentCaptor.forClass(java.util.function.Consumer.class);
        verify(firstClient.getEventManager()).onEvent(eq(ChannelMessageEvent.class), handlerCaptor.capture());

        ChannelMessageEvent event = mock(ChannelMessageEvent.class, RETURNS_DEEP_STUBS);
        when(event.getMessage()).thenReturn("?help");
        when(event.getUser().getName()).thenReturn("viewer");
        handlerCaptor.getValue().accept(event);

        verify(firstClient.getChat()).sendMessage("first", "response");
        verify(secondClient.getChat(), never()).sendMessage(anyString(), anyString());
    }

    private static ChannelAuthorization authorization(String id, String login) {
        return new ChannelAuthorization(
                "twitch", id, login, "access-token", "refresh-token", List.of(),
                Instant.now().plusSeconds(3600), ChannelAuthorization.Status.AUTHORIZED,
                Instant.now(), Instant.now());
    }
}
