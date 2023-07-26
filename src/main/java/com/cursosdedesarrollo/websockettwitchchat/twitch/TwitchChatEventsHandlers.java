package com.cursosdedesarrollo.websockettwitchchat.twitch;


import com.cursosdedesarrollo.websockettwitchchat.jsonconfig.Command;
import com.cursosdedesarrollo.websockettwitchchat.jsonconfig.JsonConfigService;
import com.cursosdedesarrollo.websockettwitchchat.websocket.MyWebSocketHandler;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
public class TwitchChatEventsHandlers {

    @Autowired
    private TwitchHelixClient twitchHelixClient;

    @Autowired
    private JsonConfigService configuracionService;

    private static final Logger logger = LoggerFactory.getLogger(TwitchChatEventsHandlers.class);
    public void executeCommands(ChannelMessageEvent event, TwitchChatClient bot, String channel, MyWebSocketHandler myWebSocketHandler) {
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
                bot.sendMessage(channel,"Ayuda: puedes usar los comandos: !youtube !redes !java !javaweb !angular !github !discord");
            }
            if (message.startsWith("!grabando")){
                bot.sendMessage(channel,"Estamos grabando un curso para Youtube. Mientras tanto no podemos atender al chat. Cuando terminemos podemos atender tus preguntas.");
            }
            if (message.startsWith("!youtube")){
                bot.sendMessage(channel,"El canal de Youtube es: https://www.youtube.com/@CursosdeDesarrollo , Canal secundario: https://www.youtube.com/@CursosDesencadenado");
            }
            if (message.startsWith("!redes")){
                bot.sendMessage(channel,"Enlaces de contacto: https://twitter.com/dvaquero , https://twitter.com/CDDesarrollo y https://www.linkedin.com/in/davidvaquero/");
            }
            if (message.startsWith("!java")){
                bot.sendMessage(channel,"Este es el enlace al curso de java en Youtube: https://www.youtube.com/watch?v=JExfQrDN03k&list=PLd7FFr2YzghOjHnoLF_yLjjOFnknA8qJj");
            }
            if (message.startsWith("!javaweb")){
                bot.sendMessage(channel,"Este es el enlace al curso de java Web en Youtube: https://www.youtube.com/playlist?list=PLd7FFr2YzghNRITcQTMsuW5puOPGH3-PV");
            }
            if (message.startsWith("!typescript")){
                bot.sendMessage(channel,"Este es el enlace al curso de Typescript en Youtube https://www.youtube.com/playlist?list=PLd7FFr2YzghNmAgEDpfZeWsb5DuiOLHbB  y el repositorio de código: https://github.com/pepesan/curso-typescript-twitch");
            }
            if (message.startsWith("!angular")){
                bot.sendMessage(channel,"Este es el enlace al curso de angular en Youtube: https://www.youtube.com/watch?v=UGBWmShB4J8&list=PLd7FFr2YzghNPi66KMyBbrBmJzH-RPYz0");
            }
            if (message.startsWith("!ansible")){
                bot.sendMessage(channel,"Este es el enlace al curso de ansible en Youtube: https://www.youtube.com/watch?v=aYfqoh1PhBk&list=PLd7FFr2YzghNETVzT99w0hiWUDRNsqkq6");
            }
            if (message.startsWith("!github")){
                bot.sendMessage(channel,"El perfil de github es: https://github.com/pepesan");
            }
            if (message.startsWith("!discord")){
                bot.sendMessage(channel,"El servidor de discord está en: https://discord.gg/9eWkvyR");
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

                    bot.sendMessage(channel,cadenaSalida);
                }
            }
        }
    }
}
