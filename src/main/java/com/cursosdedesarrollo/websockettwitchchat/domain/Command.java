package com.cursosdedesarrollo.websockettwitchchat.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.TableGenerator;
import lombok.Data;

@Data
@Entity
public class Command {
    @Id
    @TableGenerator(
            name = "command_ids",
            table = "command_seq",
            pkColumnName = "sequence_name",
            valueColumnName = "next_val",
            pkColumnValue = "command",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "command_ids")
    private Long id;
    private String name;
    private String outputText;
    private String outputType;
    private Boolean ownerOnly;
    private Boolean onlyMods;

    public Command(){
        this.id = null;
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
