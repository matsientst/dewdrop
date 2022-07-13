package events.dewdrop.structure.api;

import events.dewdrop.structure.events.CorrelationCausation;
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
