package events.dewdrop.fixture.events.user;

import events.dewdrop.aggregate.annotation.AggregateId;
import events.dewdrop.structure.api.Event;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "userId")
public abstract class UserEvent extends Event {
    @AggregateId
    private UUID userId;

    public UserEvent(UUID userId) {
        this.userId = userId;
    }
}
