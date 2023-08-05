package com.cursosdedesarrollo.websockettwitchchat.domain;

import com.cursosdedesarrollo.websockettwitchchat.controllers.CallBackController;
import com.cursosdedesarrollo.websockettwitchchat.domain.Command;
import com.cursosdedesarrollo.websockettwitchchat.domain.Scope;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

@Data
public class Configuracion {
    private static final Logger logger = LoggerFactory.getLogger(Configuracion.class);

    private String commandSymbol;
    private List<Command> commands;
    private List<Scope> scopes;

    public Configuracion(JSONObject jsonObject){
        this.commandSymbol = jsonObject.getString("command_symbol");
        this.commands = new LinkedList<>();
        JSONArray jsonArray = jsonObject.getJSONArray("commands");
        for( Object object : jsonArray ){
           JSONObject commandJSON= (JSONObject) object;
           Command command = new Command();
           command.setName(commandJSON.getString("name"));
           command.setOutputText(commandJSON.getString("output_text"));
           command.setOutputType(commandJSON.getString("output_type"));
           command.setOwnerOnly(commandJSON.getBoolean("owner_only"));
           command.setOnlyMods(commandJSON.getBoolean("mods_only"));
           this.commands.add(command);
        }
        logger.info("commands: {}", commands);
    }

    public Command buscarComando(String comandoBuscado) {
        for (Command comando : this.commands) {
            if (comando.getName().equalsIgnoreCase(comandoBuscado)) {
                return comando;
            }
        }
        return null; // Comando no encontrado en la lista
    }
}

