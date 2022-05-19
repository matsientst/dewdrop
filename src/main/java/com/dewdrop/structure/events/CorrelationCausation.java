package com.dewdrop.structure.events;

import com.dewdrop.structure.api.AbstractMessage;
import com.dewdrop.structure.api.Message;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;
import lombok.Data;

@Data
public abstract class CorrelationCausation extends AbstractMessage {
    @JsonIgnore
    protected UUID correlationId;
    @JsonIgnore
    protected UUID causationId;

    public CorrelationCausation() {
        super();
        this.causationId = null;
        this.correlationId = UUID.randomUUID();
    }

}
