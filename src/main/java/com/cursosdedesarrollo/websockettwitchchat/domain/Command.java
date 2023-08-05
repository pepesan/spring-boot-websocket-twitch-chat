package com.cursosdedesarrollo.websockettwitchchat.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Command {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String outputText;
    private String outputType;
    private Boolean ownerOnly;
    private Boolean onlyMods;

    public Command(){
        this.id = 0L;
        this.name = "";
        this.outputText = "";
        this.outputType = "";
        this.ownerOnly = false;
        this.onlyMods = false;
    }

    public void fromCommand(Command c){
        this.name = c.getName();
        this.outputText = c.getOutputText();
        this.outputType = c.getOutputType();
        this.ownerOnly = c.getOwnerOnly();
        this.onlyMods = c.getOnlyMods();
    }
}
