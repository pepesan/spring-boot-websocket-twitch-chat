package com.cursosdedesarrollo.websockettwitchchat.jsonconfig;

import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

@Data
public class Configuracion {
    private String commandSymbol;
    private List<Command> commands;

    Configuracion(JSONObject jsonObject){
        this.commandSymbol = jsonObject.getString("command_symbol");
        this.commands = new LinkedList<>();
        JSONArray jsonArray = jsonObject.getJSONArray("commands");
        for( Object object : jsonArray ){
           JSONObject commandJSON= (JSONObject) object;
           Command command = new Command();
           command.setName(commandJSON.getString("name"));
           command.setOutputText(commandJSON.getString("output_text"));
           command.setOwnerOnly(commandJSON.getBoolean("owner_only"));
           command.setOnlyMods(commandJSON.getBoolean("only_mods"));
           this.commands.add(command);
        }
    }

    // getters y setters
}

