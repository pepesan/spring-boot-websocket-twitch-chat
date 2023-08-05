package com.cursosdedesarrollo.websockettwitchchat.services;

import com.cursosdedesarrollo.websockettwitchchat.domain.Command;
import com.cursosdedesarrollo.websockettwitchchat.domain.Configuracion;
import com.cursosdedesarrollo.websockettwitchchat.repositories.CommandRepository;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class JsonConfigService {

    private CommandRepository commandRepository;

    private static final Logger logger = LoggerFactory.getLogger(JsonConfigService.class);

    public CommandRepository getCommandRepository() {
        return commandRepository;
    }

    public void setCommandRepository(CommandRepository commandRepository) {
        this.commandRepository = commandRepository;
    }

    public Configuracion getConfiguracion() {
        return configuracion;
    }

    public void setConfiguracion(Configuracion configuracion) {
        this.configuracion = configuracion;
    }

    private Configuracion configuracion;

    @Autowired
    public JsonConfigService(CommandRepository commandRepository) throws IOException {
        this.commandRepository = commandRepository;
        ClassPathResource staticDataResource = new ClassPathResource("commands.json");
        String staticDataString = IOUtils.toString(staticDataResource.getInputStream(), StandardCharsets.UTF_8);
        logger.info(staticDataString);
        JSONObject jsonObject = new JSONObject(staticDataString);
        this.configuracion = new Configuracion(jsonObject);
        for (Command c : this.configuracion.getCommands()){
            Command posibleCommand = this.commandRepository.findByName(c.getName());
            if (posibleCommand== null){
                this.commandRepository.save(c);
            }else{
                posibleCommand.fromCommand(c);
                this.commandRepository.save(posibleCommand);
            }
        }
        logger.info(this.configuracion.toString());
    }

}
