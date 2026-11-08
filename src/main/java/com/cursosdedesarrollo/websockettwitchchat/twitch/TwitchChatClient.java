package com.cursosdedesarrollo.websockettwitchchat.twitch;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.domain.Command;
import com.cursosdedesarrollo.websockettwitchchat.commands.JsonConfigService;
import com.cursosdedesarrollo.websockettwitchchat.twitch.events.TwitchEventSubService;
import com.cursosdedesarrollo.websockettwitchchat.websocket.MyWebSocketHandler;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.api.domain.IDisposable;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.chat.events.channel.*;
import com.github.twitch4j.eventsub.events.*;
import com.github.twitch4j.helix.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TwitchChatClient {



    private static final Logger logger = LoggerFactory.getLogger(TwitchChatClient.class);
    private final Map<String, TwitchConnection> connections = new ConcurrentHashMap<>();

    private final JsonConfigService configuracionService;
    private final TwitchHelixClient twitchHelixClient;
    private final MyWebSocketHandler myWebSocketHandler;
    private final TwitchEventSubService twitchEventSubService;
    private final TwitchClientFactory twitchClientFactory;

    public TwitchChatClient(
            JsonConfigService configuracionService,
            TwitchHelixClient twitchHelixClient,
            MyWebSocketHandler myWebSocketHandler,
            TwitchEventSubService twitchEventSubService,
            TwitchClientFactory twitchClientFactory) {
        this.configuracionService = configuracionService;
        this.twitchHelixClient = twitchHelixClient;
        this.myWebSocketHandler = myWebSocketHandler;
        this.twitchEventSubService = twitchEventSubService;
        this.twitchClientFactory = twitchClientFactory;
    }

    private static final Set<String> DEFAULT_MODERATORS =
            Set.of("cursosdedesarrollo", "RonTxT", "SistemasItPro", "lufegaba");

    private String calculateUptime(TwitchConnection connection) {
        long elapsedSeconds = Math.max(0, (System.currentTimeMillis() - connection.startedAt.getTime()) / 1000);
        long minutes = elapsedSeconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        StringBuilder output = new StringBuilder("Tiempo de stream: ");
        if (days > 0) {
            output.append("Días: ").append(days).append(", ");
        }
        if (hours > 0) {
            output.append("Horas: ").append(hours % 24).append(", ");
        }
        if (minutes > 0) {
            output.append("Minutos: ").append(minutes % 60).append(", ");
        }
        if (elapsedSeconds > 0) {
            output.append("Segundos: ").append(elapsedSeconds % 60).append(".");
        }
        return output.toString();
    }


    public synchronized void connect(ChannelAuthorization authorization) {
        if (!"twitch".equals(authorization.providerId())) {
            throw new IllegalArgumentException("Authorization belongs to a different chat provider");
        }
        String providerUserId = authorization.providerUserId();
        disconnect(providerUserId);
        OAuth2Credential credential = new OAuth2Credential("twitch", authorization.accessToken());
        logger.info("Bot Chat inicializando para el canal {}", authorization.login());
        TwitchClient client = twitchClientFactory.create(credential);
        TwitchConnection connection = new TwitchConnection(authorization, client);
        connections.put(providerUserId, connection);
        try {
            registerEvents(connection);
            twitchEventSubService.registerChannelSubscriptions(client.getEventSocket(), authorization);
            logger.info("Bot Chat conectando para el canal {}", authorization.login());
        } catch (RuntimeException exception) {
            disconnect(providerUserId);
            throw exception;
        }
    }

    public void getChannelMods(String providerUserId) {
        TwitchConnection connection = requireConnection(providerUserId);
        List<Moderator> moderators = connection.client
                .getHelix()
                .getModerators(connection.authorization.accessToken(), providerUserId, null, null, null)
                .execute()
                .getModerators();
        Set<String> names = new HashSet<>(moderators.size());
        for (Moderator moderator : moderators) {
            names.add(moderator.getUserName());
        }
        connection.moderators = names;
        logger.info("Loaded moderators for Twitch channel {}", connection.authorization.login());
    }

    public void connectAndJoinChannel(String providerUserId) {
        TwitchConnection connection = requireConnection(providerUserId);
        connection.client.getChat().joinChannel(connection.authorization.login());
    }

    private void registerEvents(TwitchConnection connection) {
        logger.info("Registrando eventos para el canal {}", connection.authorization.login());
        connection.messageHandler = connection.client
                .getEventManager()
                .onEvent(ChannelMessageEvent.class, event -> {
                    logger.info("[{}][{}] {}: {}", event.getChannel().getName(),
                            event.getPermissions(), event.getUser().getName(), event.getMessage());
                    String commandSymbol = configuracionService.getConfiguracion().getCommandSymbol();
                    if (commandSymbol != null && !commandSymbol.isBlank()
                            && event.getMessage().startsWith(commandSymbol)) {
                        executeCommands(event, connection);
                    } else {
                        try {
                            myWebSocketHandler.sendMessage(event);
                        } catch (IOException exception) {
                            logger.error("Could not forward Twitch chat message to WebSocket");
                        }
                    }
                });
        connection.client.getEventManager().onEvent(ChannelFollowEvent.class,
                event -> logEventSub("channel.follow", connection.authorization));
        connection.client.getEventManager().onEvent(ChannelCheerEvent.class,
                event -> logEventSub("channel.cheer", connection.authorization));
        connection.client.getEventManager().onEvent(ChannelRaidEvent.class,
                event -> logEventSub("channel.raid", connection.authorization));
        connection.client.getEventManager().onEvent(ChannelSubscribeEvent.class,
                event -> logEventSub("channel.subscribe", connection.authorization));
        connection.client.getEventManager().onEvent(ChannelSubscriptionGiftEvent.class,
                event -> logEventSub("channel.subscription.gift", connection.authorization));
        connection.client.getEventManager().onEvent(ChannelSubscriptionMessageEvent.class,
                event -> logEventSub("channel.subscription.message", connection.authorization));
    }

    private void logEventSub(String eventType, ChannelAuthorization authorization) {
        logger.info("Twitch EventSub {} received for channel {}", eventType, authorization.login());
    }

    private void sendMessage(TwitchConnection connection, String message) {
        connection.client.getChat().sendMessage(connection.authorization.login(), message);
    }

    public boolean isConnected(String providerUserId) {
        return connections.containsKey(providerUserId);
    }

    public synchronized void disconnect(String providerUserId) {
        TwitchConnection connection = connections.remove(providerUserId);
        if (connection == null) {
            return;
        }
        if (connection.messageHandler != null) {
            connection.messageHandler.dispose();
        }
        connection.client.close();
    }

    private TwitchConnection requireConnection(String providerUserId) {
        TwitchConnection connection = connections.get(providerUserId);
        if (connection == null) {
            throw new IllegalStateException("Twitch channel is not connected");
        }
        return connection;
    }
    private void executeCommands(ChannelMessageEvent event, TwitchConnection connection) {
        String message = event.getMessage();
        ChannelAuthorization authorization = connection.authorization;
        String commandSymbol = configuracionService.getConfiguracion().getCommandSymbol();
        if (commandSymbol != null && !commandSymbol.isBlank() && message.startsWith(commandSymbol)) {
            String[] words = message.trim().split("\\s+");
            if (words.length > 0 && words[0].startsWith(commandSymbol)) {
                String commandName = words[0].substring(commandSymbol.length());
                Command command = configuracionService.getConfiguracion().buscarComando(commandName);
                if (command != null && comprobamosPermisos(command, event.getUser().getName(), connection)
                        && "normal".equals(command.getOutputType())) {
                    sendMessage(connection, command.getOutputText());
                }
            }
        }

        String userName = event.getUser().getName();
        if (!userName.equalsIgnoreCase(authorization.login())
                && !connection.moderators.contains(userName)) {
            return;
        }
        if (message.startsWith("!subs")) {
            try {
                List<Subscription> subscriptions = connection.client.getHelix()
                        .getSubscriptions(authorization.accessToken(), authorization.providerUserId(), null, null, null)
                        .execute()
                        .getSubscriptions();
                logger.info("Subscription lookup completed for channel {} ({} results)",
                        authorization.login(), subscriptions.size());
            } catch (Exception exception) {
                logger.error("Twitch subscription lookup failed for channel {} ({})",
                        authorization.login(), exception.getClass().getSimpleName());
            }
        }
        if (message.startsWith("!mods")) {
            getChannelMods(authorization.providerUserId());
        }
        if (message.startsWith("!uptime")) {
            sendMessage(connection, calculateUptime(connection));
        }
        if (message.startsWith("!so")) {
            sendShoutout(connection, message.substring(4));
        }
    }

    private void sendShoutout(TwitchConnection connection, String channelName) {
        String streamTitle = null;
        String categoryName = null;
        try {
            List<User> users = twitchHelixClient.client.getUsers(
                    null, null, Collections.singletonList(channelName)).execute().getUsers();
            if (users.isEmpty()) {
                return;
            }
            String userId = users.getFirst().getId();
            StreamList resultList = twitchHelixClient.client.getStreams(
                    null, null, null, 5, null, null, Collections.singletonList(userId), null).execute();
            List<Stream> streams = resultList.getStreams();
            if (!streams.isEmpty()) {
                streamTitle = streams.getFirst().getTitle();
                categoryName = streams.getFirst().getGameName();
            } else {
                streamTitle = "Sin titulo disponible";
                categoryName = "Sin Categoría Disponible";
            }
            VideoList videos = twitchHelixClient.client.getVideos(
                    null, null, userId, null, null, null, null, null, 5, null, null).execute();
            List<Video> videoList = videos.getVideos();
            if (!videoList.isEmpty() && "Sin titulo disponible".equals(streamTitle)) {
                streamTitle = videoList.getFirst().getTitle();
            }
        } catch (Exception exception) {
            logger.error("Twitch shoutout lookup failed ({})", exception.getClass().getSimpleName());
        }
        if (streamTitle != null && !streamTitle.isBlank()) {
            String output = "Echale un vistazo al canal de https://twitch.tv/" + channelName
                    + " . El último video fue sobre: " + streamTitle;
            if (categoryName != null && !"Sin Categoría Disponible".equals(categoryName)) {
                output += " y suele streamear en la categoría: " + categoryName;
            }
            sendMessage(connection, output);
        }
    }

    private boolean comprobamosPermisos(Command command, String user, TwitchConnection connection) {
        if (command.getOwnerOnly() && user.equalsIgnoreCase(connection.authorization.login())) {
            return true;
        }
        if (command.getOnlyMods() && connection.moderators.contains(user)) {
            return true;
        }
        return !command.getOwnerOnly() && !command.getOnlyMods();
    }

    private static final class TwitchConnection {
        private final ChannelAuthorization authorization;
        private final TwitchClient client;
        private final Date startedAt = new Date();
        private volatile Set<String> moderators = new HashSet<>(DEFAULT_MODERATORS);
        private volatile IDisposable messageHandler;

        private TwitchConnection(ChannelAuthorization authorization, TwitchClient client) {
            this.authorization = authorization;
            this.client = client;
        }
    }
}
