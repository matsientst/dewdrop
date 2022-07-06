package org.dewdrop.read.readmodel;

import static java.util.Objects.requireNonNull;

import org.dewdrop.structure.api.Event;
import lombok.Getter;

@Getter
public class ReadModelConstructed {
    private boolean ephemeral = false;
    private int destroyInMinutesUnused = -1;
    private ReadModel<Event> readModel;

    public ReadModelConstructed(ReadModel<Event> readModel) {
        requireNonNull(readModel, "readModel is required");

        org.dewdrop.read.readmodel.annotation.ReadModel annotation = readModel.getReadModelWrapper().getOriginalReadModelClass().getAnnotation(org.dewdrop.read.readmodel.annotation.ReadModel.class);
        this.ephemeral = annotation.ephemeral();
        this.destroyInMinutesUnused = annotation.destroyInMinutesUnused();
        this.readModel = readModel;
    }

}
