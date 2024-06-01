package com.cursosdedesarrollo.websockettwitchchat.twitch;

import com.cursosdedesarrollo.websockettwitchchat.domain.Command;
import com.cursosdedesarrollo.websockettwitchchat.domain.TwitchConfig;
import com.cursosdedesarrollo.websockettwitchchat.services.JsonConfigService;
import com.cursosdedesarrollo.websockettwitchchat.websocket.MyWebSocketHandler;
import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.chat.events.channel.*;
import com.github.twitch4j.eventsub.events.ChannelSubscribeEvent;
import com.github.twitch4j.helix.domain.*;
import com.github.twitch4j.pubsub.events.ChannelBitsEvent;
import com.github.twitch4j.pubsub.events.ChannelPointsRedemptionEvent;
import com.github.twitch4j.pubsub.events.ChannelSubGiftEvent;
import com.github.twitch4j.pubsub.events.CharityCampaignDonationEvent;
import com.github.twitch4j.pubsub.events.CheerbombEvent;
import com.github.twitch4j.pubsub.events.FollowingEvent;
import com.github.twitch4j.pubsub.events.LowTrustUserNewMessageEvent;
import com.github.twitch4j.pubsub.events.PubSubConnectionStateEvent;
import com.github.twitch4j.pubsub.events.PointsSpentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.api.domain.IDisposable;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.Duration;

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


    public void connect() {
//        CredentialManager credentialManager = CredentialManagerBuilder.builder()
//                .build();
//        credentialManager.registerIdentityProvider(new TwitchIdentityProvider(this.twitchConfig.getClientId(), this.twitchConfig.getClientSecret(), this.twitchConfig.getRedirectUri()));
        oAuth2Credential = new OAuth2Credential("twitch", this.twitchConfig.getOauthToken());
        logger.info("Bot Chat inicializando");
        twitchClient = TwitchClientBuilder.builder()
                .withEnableHelix(true)
                .withEnableChat(true)
                .withEnableEventSocket(true)
                .withEnablePubSub(true)
                .withChatAccount(oAuth2Credential)
                .withDefaultEventHandler(SimpleEventHandler.class)
                //.withCredentialManager(credentialManager)
                .build();
        // pubsub
        twitchClient.getPubSub().listenForFollowingEvents(oAuth2Credential, this.twitchConfig.getChannelId());
        twitchClient.getPubSub().listenForCheerEvents(oAuth2Credential, this.twitchConfig.getChannelId());
        twitchClient.getPubSub().listenForRaidEvents(oAuth2Credential, this.twitchConfig.getChannelId());
        twitchClient.getPubSub().listenForChannelSubGiftsEvents(oAuth2Credential, this.twitchConfig.getChannelId());
        twitchClient.getPubSub().listenForSubscriptionEvents(oAuth2Credential, this.twitchConfig.getChannelId());
        //twitchClient.getPubSub().listenForUserChannelPointsEvents(oAuth2Credential, this.twitchConfig.getChannelId());
        logger.info("Bot Chat conectando");
    }
    public void getChannelMods(){
        this.mods = new HashSet<>();
        List<Moderator> listMods = twitchClient
                .getHelix()
                .getModerators(this.twitchConfig.getOauthToken(), this.twitchConfig.getChannelId(), null, null, null)
                .execute()
                .getModerators();
        for(Moderator mod : listMods){
            this.mods.add(mod.getUserName());
        }
        logger.info("Mods: {}", this.mods);
    }

    public void connectAndJoinChannel() {
        twitchClient.getChat().joinChannel(this.twitchConfig.getChannel());
        registerEvents();
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
                                this.executeCommands(event, this.twitchConfig.getChannel(), myWebSocketHandler);
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
        twitchClient.getEventManager().onEvent(RaidEvent.class,
                event -> {
                    logger.info(event.toString());
                });
        twitchClient.getEventManager().onEvent(ChannelBitsEvent.class,
                event -> {
                    logger.info(event.toString());
                });
        twitchClient.getEventManager().onEvent(ChannelJoinEvent.class,
                event -> {
                    logger.info(event.toString());
                });
        twitchClient.getEventManager().onEvent(ChannelLeaveEvent.class,
                event -> {
                    logger.info(event.toString());
                });
        twitchClient.getEventManager().onEvent(ChannelModEvent.class,
                event -> {
                    logger.info(event.toString());
                });
        twitchClient.getEventManager().onEvent(ChannelNoticeEvent.class,
                event -> {
                    logger.info(event.toString());
                });
        twitchClient.getEventManager().onEvent(ChannelPointsRedemptionEvent.class,
                event -> {
                    logger.info(event.toString());
                });
        twitchClient.getEventManager().onEvent(ChannelMessageActionEvent.class,
                event -> {
                    logger.info(event.toString());
                });
        twitchClient.getEventManager().onEvent(ChannelSubGiftEvent.class,
                event -> {
                    logger.info(event.toString());
                });
        twitchClient.getEventManager().onEvent(ChannelSubscribeEvent.class,
                event -> {
                    logger.info(event.toString());
                });
        twitchClient.getEventManager().onEvent(CharityCampaignDonationEvent.class,
                event -> {
                    logger.info(event.toString());
                });
        twitchClient.getEventManager().onEvent(CheerbombEvent.class,
                event -> {
                    logger.info(event.toString());
                });
        twitchClient.getEventManager().onEvent(FollowingEvent.class,
                event -> {
                    logger.info(event.toString());
                });
        twitchClient.getEventManager().onEvent(LowTrustUserNewMessageEvent.class,
                event -> {
                    logger.info(event.toString());
                });
        twitchClient.getEventManager().onEvent(PubSubConnectionStateEvent.class,
                event -> {
                    logger.info(event.toString());
                });
        twitchClient.getEventManager().onEvent(PointsSpentEvent.class,
                event -> {
                    logger.info(event.toString());
                });
        twitchClient.getEventManager().onEvent(PointsSpentEvent.class,
                event -> {
                    logger.info(event.toString());
                });
    }

    public void sendMessage(String message) {
        logger.info("Mandando el primer mensaje al chat");
        twitchClient.getChat().sendMessage(this.twitchConfig.getChannel(), message);
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
            if(event.getUser().getName().equals(this.twitchConfig.getChannel()) || this.mods.contains(event.getUser().getName())){
                if(message.startsWith("!subs")){
                    logger.info("!subs");
                    String channelName = this.twitchConfig.getChannel();
                    try {
//                    List<User> users = twitchHelixClient.client.getUsers(
//                            null,
//                            null,
//                            Collections.singletonList(channelName)).execute().getUsers();
//                    logger.info("users: {}", users);
//                    String channelToString = users.get(0).toString();
                        String userId = oAuth2Credential.getUserId();
                        logger.info("userId: {}", userId);
                        List<Subscription> subs = this.twitchClient.getHelix().getSubscriptions(this.twitchConfig.getOauthToken(), userId, null, null, null).execute().getSubscriptions();
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
        if (c.getOwnerOnly() && user.equals(twitchConfig.getChannel())){
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
