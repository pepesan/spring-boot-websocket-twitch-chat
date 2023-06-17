package com.cursosdedesarrollo.websockettwitchchat.twitch;


import com.cursosdedesarrollo.websockettwitchchat.websocket.MyWebSocketHandler;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;

@Component
public class TwitchChatEventsHandlers {

    private static final Logger logger = LoggerFactory.getLogger(TwitchChatEventsHandlers.class);
    public void executeCommands(ChannelMessageEvent event, TwitchChatClient bot, String channel, MyWebSocketHandler myWebSocketHandler) {
        // System.out.println "[" + event.getChannel().getName() + "]["+event.getPermissions().toString()+"] " + event.getUser().getName() + ": " + event.getMessage());
        if (event.getUser().getName().equals(channel)){
            logger.info("Channel Owner Message");
            String message = event.getMessage();
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
            if (message.startsWith("!github")){
                bot.sendMessage(channel,"El servidor de discord está en: https://discord.gg/9eWkvyR");
            }
            if (message.startsWith("!so")){
                String channelName = message.substring(4);
                // String channelToString = bot.getChannelID(channelName);
                // logger.info("channelToString: " + channelToString);
                bot.sendMessage(channel,"Echale un vistazo al canal de https://twitch.tv/"+channelName
                        // + ". El último video fue sobre: {video.title}"
                );
            }
        }
    }
}
