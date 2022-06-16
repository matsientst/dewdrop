package com.dewdrop.structure.api;

import com.dewdrop.structure.events.CorrelationCausation;
import java.util.UUID;
import lombok.Data;

@Data
public abstract class Event extends CorrelationCausation {
    private Long version;

    public Event() {}

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
