package events.dewdrop.structure.api;

import java.util.UUID;

import events.dewdrop.structure.events.CorrelationCausation;
import lombok.Data;

@Data
public abstract class Event extends CorrelationCausation {
    private Long version;
    private UUID eventId;

    public Event() {}

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }
}
