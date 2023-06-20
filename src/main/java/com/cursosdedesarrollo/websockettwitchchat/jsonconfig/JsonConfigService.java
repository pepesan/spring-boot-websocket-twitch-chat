package com.cursosdedesarrollo.websockettwitchchat.jsonconfig;

import com.cursosdedesarrollo.websockettwitchchat.twitch.TwitchChatClient;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class JsonConfigService {
    private static final Logger logger = LoggerFactory.getLogger(JsonConfigService.class);
    JsonConfigService() throws IOException {
        ClassPathResource staticDataResource = new ClassPathResource("commands.json");
        String staticDataString = IOUtils.toString(staticDataResource.getInputStream(), StandardCharsets.UTF_8);
        logger.info(staticDataString);
        JSONObject jsonObject = new JSONObject(staticDataString);
        Configuracion configuracion = new Configuracion(jsonObject);
        logger.info(configuracion.toString());
    }
}
