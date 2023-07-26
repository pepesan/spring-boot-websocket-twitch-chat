package com.cursosdedesarrollo.websockettwitchchat.jsonconfig;

import lombok.Data;

@Data
public class Command {
    private String name;
    private String outputText;
    private Boolean ownerOnly;
    private Boolean onlyMods;
}
