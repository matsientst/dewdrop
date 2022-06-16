package com.dewdrop.read.readmodel;

import static java.util.Objects.requireNonNull;

import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.api.Message;
import lombok.Getter;

@Getter
public class ReadModelConstructed {
    private boolean ephemeral = false;
    private int destroyInMinutesUnused = -1;
    private ReadModel<Event> readModel;

    public ReadModelConstructed(ReadModel<Event> readModel) {
        requireNonNull(readModel, "readModel is required");

        com.dewdrop.read.readmodel.annotation.ReadModel annotation = readModel.getReadModel().getClass().getAnnotation(com.dewdrop.read.readmodel.annotation.ReadModel.class);
        this.ephemeral = annotation.ephemeral();
        this.destroyInMinutesUnused = annotation.destroyInMinutesUnused();
        this.readModel = readModel;
    }

}
