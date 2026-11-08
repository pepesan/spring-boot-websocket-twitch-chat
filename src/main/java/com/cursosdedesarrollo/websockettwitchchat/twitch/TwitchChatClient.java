package com.cursosdedesarrollo.websockettwitchchat.twitch;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.domain.Command;
import com.cursosdedesarrollo.websockettwitchchat.domain.TwitchConfig;
import com.cursosdedesarrollo.websockettwitchchat.services.JsonConfigService;
import com.cursosdedesarrollo.websockettwitchchat.services.TwitchEventSubService;
import com.cursosdedesarrollo.websockettwitchchat.websocket.MyWebSocketHandler;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.api.domain.IDisposable;
import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.*;
import com.github.twitch4j.eventsub.events.*;
import com.github.twitch4j.helix.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class TwitchChatClient {

    @Autowired
    private JsonConfigService configuracionService;

    @Autowired
    private TwitchHelixClient twitchHelixClient;

    public TwitchClient twitchClient;
    private static final Logger logger = LoggerFactory.getLogger(TwitchChatClient.class);

    private Date fechaHoraInicio;


    @Autowired
    private MyWebSocketHandler myWebSocketHandler;
    IDisposable handlerOnMessage;

    @Autowired
    private TwitchConfig twitchConfig;
    @Autowired
    private TwitchEventSubService twitchEventSubService;
    private ChannelAuthorization activeAuthorization;
    public OAuth2Credential oAuth2Credential;

    public Set<String> mods;

    public

    TwitchChatClient(){
        this.mods = Set.of("cursosdedesarrollo","RonTxT", "SistemasItPro", "lufegaba");
        this.fechaHoraInicio = obtenerFechaHoraInicial();
    }

    public static Date obtenerFechaHoraInicial() {
        return new Date(); // Esto obtendrá la fecha y hora actual
    }

    // Función para calcular el tiempo transcurrido desde la fecha y hora inicial
    public String calcularTiempoTranscurrido() {
        Date fechaHoraActual = new Date(); // Obtener la fecha y hora actual
        long tiempoTranscurridoMillis = fechaHoraActual.getTime() - this.fechaHoraInicio.getTime();

        // Convertir los milisegundos a segundos, minutos, horas y días
        long segundos = tiempoTranscurridoMillis / 1000;
        long minutos = segundos / 60;
        long horas = minutos / 60;
        long dias = horas / 24;
        StringBuffer sb = new StringBuffer();
        sb.append("Tiempo de stream: ");
        if (dias>0) {
            sb.append("Días: ").append(dias).append(", ");
        }
        if (horas>0) {
            sb.append("Horas: ").append(horas % 24).append(", ");
        }
        if (minutos>0) {
            sb.append("Minutos: ").append(minutos % 60).append(", ");
        }
        if (segundos>0) {
            sb.append("Segundos: ").append(segundos % 60).append(".");
        }
        return sb.toString();
    }


    public void connect(ChannelAuthorization authorization) {
        if (!"twitch".equals(authorization.providerId())) {
            throw new IllegalArgumentException("Authorization belongs to a different chat provider");
        }
        this.activeAuthorization = authorization;
        oAuth2Credential = new OAuth2Credential("twitch", authorization.accessToken());
        logger.info("Bot Chat inicializando para el canal {}", authorization.login());
        twitchClient = TwitchClientBuilder.builder()
                .withClientId(twitchConfig.getClientId())
                .withClientSecret(twitchConfig.getClientSecret())
                .withEnableHelix(true)
                .withEnableChat(true)
                .withEnableEventSocket(true)
                .withChatAccount(oAuth2Credential)
                .withDefaultAuthToken(oAuth2Credential)
                .withDefaultEventHandler(SimpleEventHandler.class)
                .build();
        registerEvents();
        twitchEventSubService.registerChannelSubscriptions(twitchClient.getEventSocket(), authorization);
        logger.info("Bot Chat conectando para el canal {}", authorization.login());
    }
    public void getChannelMods(){
        this.mods = new HashSet<>();
        List<Moderator> listMods = twitchClient
                .getHelix()
                .getModerators(activeAuthorization.accessToken(), activeAuthorization.providerUserId(), null, null, null)
                .execute()
                .getModerators();
        for(Moderator mod : listMods){
            this.mods.add(mod.getUserName());
        }
        logger.info("Mods: {}", this.mods);
    }

    public void connectAndJoinChannel() {
        twitchClient.getChat().joinChannel(activeAuthorization.login());
    }

    public void registerEvents(){
        logger.info("Registrando eventos");
        handlerOnMessage = twitchClient
                .getEventManager()
                .onEvent(
                        ChannelMessageEvent.class,
                        event -> {
                            logger.info("[" + event.getChannel().getName() + "]["+event.getPermissions().toString()+"] " + event.getUser().getName() + ": " + event.getMessage());
                            if(event.getMessage().startsWith("!")){
                                this.executeCommands(event, activeAuthorization.login(), myWebSocketHandler);
                            }else {
                                logger.info("received message: {}", event.getMessage());
                                try {
                                    myWebSocketHandler.sendMessage(event);
                                } catch (IOException e) {
                                    logger.error("fallo al mandar mensaje al webssocket");
                                    throw new RuntimeException(e);
                                }
                            }

                        }
                );
        twitchClient.getEventManager().onEvent(ChannelFollowEvent.class,
                event -> logEventSub("channel.follow"));
        twitchClient.getEventManager().onEvent(ChannelCheerEvent.class,
                event -> logEventSub("channel.cheer"));
        twitchClient.getEventManager().onEvent(ChannelRaidEvent.class,
                event -> logEventSub("channel.raid"));
        twitchClient.getEventManager().onEvent(ChannelSubscribeEvent.class,
                event -> logEventSub("channel.subscribe"));
        twitchClient.getEventManager().onEvent(ChannelSubscriptionGiftEvent.class,
                event -> logEventSub("channel.subscription.gift"));
        twitchClient.getEventManager().onEvent(ChannelSubscriptionMessageEvent.class,
                event -> logEventSub("channel.subscription.message"));
    }

    private void logEventSub(String eventType) {
        logger.info("Twitch EventSub {} received for channel {}", eventType, activeAuthorization.login());
    }

    public void sendMessage(String message) {
        logger.info("Mandando el primer mensaje al chat");
        twitchClient.getChat().sendMessage(activeAuthorization.login(), message);
    }

    public void disconnect() {
        // cancel handler (don't call the method for new events of the required type anymore)
        handlerOnMessage.dispose();
        twitchClient.close();
    }
    public void executeCommands(ChannelMessageEvent event, String channel, MyWebSocketHandler myWebSocketHandler) {
        // System.out.println "[" + event.getChannel().getName() + "]["+event.getPermissions().toString()+"] " + event.getUser().getName() + ": " + event.getMessage());

            logger.info("Channel Owner Message");
            String message = event.getMessage();
            String commandSymbol = this.configuracionService.getConfiguracion().getCommandSymbol();
            List<Command> commandList  = this.configuracionService.getConfiguracion().getCommands();
            if (message.startsWith(commandSymbol)){
                logger.info("es un comando");
                // pillar el comando desde el texto del mensaje
                String commandName = message;
                logger.info("message: {}", message);
                // buscar el comando en el listado
                // ejecutar el comando
                String[] palabras = message.trim().split("\\s+"); // Separar la cadena en palabras
                if (palabras.length >= 1 && palabras[0].startsWith("!")) {
                    logger.info("Buscamos comando");
                    String comandoTexto = palabras[0].substring(1); // Retornar el comando sin el carácter '!'
                    // buscamos el comando el listado
                    Command comando = this.configuracionService.getConfiguracion().buscarComando(comandoTexto);
                    if (comando==null){
                        logger.error("No existe el comando");
                    }else{
                        if(comprobamosPermisos(comando, event.getUser().getName())){
                            // ejecutamos el comando
                            logger.info("tiene permisos para mandar mensaje");
                            if (comando.getOutputType().equals("normal")){
                                this.sendMessage(comando.getOutputText());
                            }
                        }

                    }
                }else{
                    logger.info("No hay comando en el json o la BBDD");
                }
            }
            if(event.getUser().getName().equals(activeAuthorization.login()) || this.mods.contains(event.getUser().getName())){
                if(message.startsWith("!subs")){
                    logger.info("!subs");
                    String channelName = activeAuthorization.login();
                    try {
//                    List<User> users = twitchHelixClient.client.getUsers(
//                            null,
//                            null,
//                            Collections.singletonList(channelName)).execute().getUsers();
//                    logger.info("users: {}", users);
//                    String channelToString = users.get(0).toString();
                        String userId = oAuth2Credential.getUserId();
                        logger.info("userId: {}", userId);
                        List<Subscription> subs = this.twitchClient.getHelix().getSubscriptions(activeAuthorization.accessToken(), userId, null, null, null).execute().getSubscriptions();
                        logger.info("subs {}", subs);
                    }catch (Exception e){
                        logger.error("Petando en la petición Helix a subs");
                        logger.error(e.getMessage());
                        for (StackTraceElement element : e.getStackTrace()){
                            logger.error(element.toString());
                        }

                    }
                }
                if (message.startsWith("!mods")){
                    this.getChannelMods();
                }
                if (message.startsWith("!uptime")){
                    this.sendMessage(calcularTiempoTranscurrido());
                }

                if (message.startsWith("!so")){
                    String channelName = message.substring(4);
                    String streamTitle = null;
                    String categoryName = null;
                    try {
                        List<User> users = twitchHelixClient.client.getUsers(
                                null,
                                null,
                                Collections.singletonList(channelName)).execute().getUsers();
                        String channelToString = users.get(0).toString();
                        String userId = users.get(0).getId();
                        logger.info("channelToString: " + channelToString);
                        logger.info("channelUserID: " + userId);
                        StreamList resultList = twitchHelixClient.client.getStreams(
                                null,
                                null,
                                null,
                                5,
                                null,
                                null,
                                Collections.singletonList(userId),
                                null).execute();

                        List<Stream> listado = resultList.getStreams();
                        logger.info("numero de streams: " + listado.size());
                        listado.forEach(stream -> {
                            String streamString = "ID: " + stream.getId() + " - Title: " + stream.getTitle() + " - Categoría: " + stream.getGameName();
                            logger.info(streamString);
                        });
                        if(listado.size()>0){
                            Stream ultimoStream = listado.get(0);
                            streamTitle = ultimoStream.getTitle();
                            categoryName = ultimoStream.getGameName();
                        }else {
                            streamTitle = "Sin titulo disponible";
                            categoryName  = "Sin Categoría Disponible";
                        }

                        VideoList resultVideoList = twitchHelixClient.client.getVideos(
                                null,
                                null,
                                userId,
                                null,
                                null,
                                null,
                                null,
                                null,
                                5,
                                null,
                                null).execute();

                        resultVideoList.getVideos().forEach( (Video video) -> {
                            logger.info(video.getId() + ": " + video.getTitle() + " - by: " + video.getUserName() + " - tostring: " + video.toString());
                        });
                        List<Video> listadoVideos = resultVideoList.getVideos();
                        logger.info("numero de videos: " + listadoVideos.size());
                        if (listadoVideos.size()>0 && streamTitle.equals("Sin titulo disponible")){
                            streamTitle = listadoVideos.get(0).getTitle();
                        }


                    }catch (Exception e){
                        logger.error("Petando en la petición Helix");
                        logger.error(e.getMessage());
                    }
                    if (streamTitle != null && !streamTitle.equals("")){
                        String cadenaSalida = "Echale un vistazo al canal de https://twitch.tv/"+channelName
                                + " . El último video fue sobre: " + streamTitle ;
                        if (categoryName!= null & !categoryName.equals("Sin Categoría Disponible")){
                            cadenaSalida+= " y suele streamear en la categoría: " + categoryName;
                        }

                        this.sendMessage(cadenaSalida);
                    }
                }
            }


    }

    private boolean comprobamosPermisos(Command c, String user) {
        if (c.getOwnerOnly() && user.equals(activeAuthorization.login())){
            return true;
        }
        if (c.getOnlyMods() && this.mods.contains(user)){
            return true;
        }
        if (!c.getOwnerOnly() && !c.getOnlyMods()){
            return true;
        }
        return false;
    }
}
