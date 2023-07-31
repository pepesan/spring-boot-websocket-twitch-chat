package com.cursosdedesarrollo.websockettwitchchat.twitch;

import com.cursosdedesarrollo.websockettwitchchat.jsonconfig.Command;
import com.cursosdedesarrollo.websockettwitchchat.jsonconfig.JsonConfigService;
import com.cursosdedesarrollo.websockettwitchchat.websocket.MyWebSocketHandler;
import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.helix.domain.*;
import com.github.twitch4j.pubsub.events.ChannelBitsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.api.domain.IDisposable;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class TwitchChatClient {

    @Autowired
    private JsonConfigService configuracionService;

    @Autowired
    private TwitchHelixClient twitchHelixClient;

    public TwitchClient twitchClient;
    private static final Logger logger = LoggerFactory.getLogger(TwitchChatClient.class);


    @Autowired
    private MyWebSocketHandler myWebSocketHandler;
    IDisposable handlerOnMessage;
    public String channel;
    public String oauthToken;
    public String clientId;
    public String clientSecret;

    public OAuth2Credential oAuth2Credential;
    public TwitchChatClient(@Value("${twitch.clientId}") String clientId,
                            @Value("${twitch.clientSecret}") String clientSecret,
                            @Value("${twitch.accessToken}") String accessToken,
                            @Value("${twitch.oauthToken}") String oauthToken,
                            @Value("${twitch.channel}") String channel) {
        this.channel = channel;
        this.oauthToken = oauthToken;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        CredentialManager credentialManager = CredentialManagerBuilder.builder()
                .build();
        credentialManager.registerIdentityProvider(new TwitchIdentityProvider(clientId, clientSecret, "http://localhost:8080/callback"));
        oAuth2Credential = new OAuth2Credential("twitch", oauthToken);
        twitchClient = TwitchClientBuilder.builder()
                .withEnableHelix(true)
                .withEnableChat(true)
                .withEnableEventSocket(true)
                .withEnablePubSub(true)
                .withChatAccount(oAuth2Credential)
                //.withCredentialManager(credentialManager)
                .build();
    }

    public void connectAndJoinChannel(String channelName) {
        twitchClient.getChat().joinChannel(channelName);
        registerEvents();
    }

    public void registerEvents(){
        handlerOnMessage = twitchClient
                .getEventManager()
                .onEvent(
                        ChannelMessageEvent.class,
                        event -> {
                            logger.info("[" + event.getChannel().getName() + "]["+event.getPermissions().toString()+"] " + event.getUser().getName() + ": " + event.getMessage());
                            if(event.getMessage().startsWith("!")){
                                this.executeCommands(event, channel, myWebSocketHandler);
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
        twitchClient.getEventManager().onEvent(ChannelBitsEvent.class,
                event -> {

                });
    }

    public void sendMessage(String channelName, String message) {
        twitchClient.getChat().sendMessage(channelName, message);
    }

    public void disconnect() {
        // cancel handler (don't call the method for new events of the required type anymore)
        handlerOnMessage.dispose();
        twitchClient.close();
    }
    public void executeCommands(ChannelMessageEvent event, String channel, MyWebSocketHandler myWebSocketHandler) {
        // System.out.println "[" + event.getChannel().getName() + "]["+event.getPermissions().toString()+"] " + event.getUser().getName() + ": " + event.getMessage());
        if (event.getUser().getName().equals(channel)){
            logger.info("Channel Owner Message");
            String message = event.getMessage();
            String commandSymbol = this.configuracionService.getConfiguracion().getCommandSymbol();
            List<Command> commandList  = this.configuracionService.getConfiguracion().getCommands();
            if (message.startsWith(commandSymbol)){
                logger.info("es un comando");
                // pillar el comando desde el texto del mensaje
                String commandName = message;
                // buscar el comando en el listado
                // ejecutar el comando
            }
            if (message.startsWith("!help")){
                this.sendMessage(channel,"Ayuda: puedes usar los comandos: !youtube !redes !java !javaweb !angular !github !discord");
            }
            if (message.startsWith("!grabando")){
                this.sendMessage(channel,"Estamos grabando un curso para Youtube. Mientras tanto no podemos atender al chat. Cuando terminemos podemos atender tus preguntas.");
            }
            if (message.startsWith("!youtube")){
                this.sendMessage(channel,"El canal de Youtube es: https://www.youtube.com/@CursosdeDesarrollo , Canal secundario: https://www.youtube.com/@CursosDesencadenado");
            }
            if (message.startsWith("!redes")){
                this.sendMessage(channel,"Enlaces de contacto: https://twitter.com/dvaquero , https://twitter.com/CDDesarrollo y https://www.linkedin.com/in/davidvaquero/");
            }
            if (message.startsWith("!java")){
                this.sendMessage(channel,"Este es el enlace al curso de java en Youtube: https://www.youtube.com/watch?v=JExfQrDN03k&list=PLd7FFr2YzghOjHnoLF_yLjjOFnknA8qJj");
            }
            if (message.startsWith("!javaweb")){
                this.sendMessage(channel,"Este es el enlace al curso de java Web en Youtube: https://www.youtube.com/playlist?list=PLd7FFr2YzghNRITcQTMsuW5puOPGH3-PV");
            }
            if (message.startsWith("!typescript")){
                this.sendMessage(channel,"Este es el enlace al curso de Typescript en Youtube https://www.youtube.com/playlist?list=PLd7FFr2YzghNmAgEDpfZeWsb5DuiOLHbB  y el repositorio de código: https://github.com/pepesan/curso-typescript-twitch");
            }
            if (message.startsWith("!angular")){
                this.sendMessage(channel,"Este es el enlace al curso de angular en Youtube: https://www.youtube.com/watch?v=UGBWmShB4J8&list=PLd7FFr2YzghNPi66KMyBbrBmJzH-RPYz0");
            }
            if (message.startsWith("!ansible")){
                this.sendMessage(channel,"Este es el enlace al curso de ansible en Youtube: https://www.youtube.com/watch?v=aYfqoh1PhBk&list=PLd7FFr2YzghNETVzT99w0hiWUDRNsqkq6");
            }
            if (message.startsWith("!github")){
                this.sendMessage(channel,"El perfil de github es: https://github.com/pepesan");
            }
            if (message.startsWith("!discord")){
                this.sendMessage(channel,"El servidor de discord está en: https://discord.gg/9eWkvyR");
            }
            if(message.startsWith("!subs")){
                logger.info("!subs");
                String channelName = twitchHelixClient.channelName;
                try {
//                    List<User> users = twitchHelixClient.client.getUsers(
//                            null,
//                            null,
//                            Collections.singletonList(channelName)).execute().getUsers();
//                    logger.info("users: {}", users);
//                    String channelToString = users.get(0).toString();
                    String userId = oAuth2Credential.getUserId();
                    logger.info("userId: {}", userId);
                    List<Subscription> subs = this.twitchClient.getHelix().getSubscriptions(this.oauthToken, userId, null, null, null).execute().getSubscriptions();
                    logger.info("subs {}", subs);
                }catch (Exception e){
                    logger.error("Petando en la petición Helix a subs");
                    logger.error(e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()){
                        logger.error(element.toString());
                    }

                }
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
                            + ". El último video fue sobre: " + streamTitle ;
                    if (categoryName!= null & !categoryName.equals("Sin Categoría Disponible")){
                        cadenaSalida+= " y suele streamear en la categoría: " + categoryName;
                    }

                    this.sendMessage(channel,cadenaSalida);
                }
            }
        }
    }
}
