package events.dewdrop.structure.api;

import java.time.Instant;
import java.util.UUID;

import events.dewdrop.structure.events.CorrelationCausation;
import lombok.Data;

@Data
public abstract class Event extends CorrelationCausation {
    // version of your event
    private Long version;
    // event number - position in stream
    private Long position;
    private UUID eventId;
    private Instant created;

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

    public Long getPosition() {
        return position;
    }

    public void setPosition(Long position) {
        this.position = position;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }
}
