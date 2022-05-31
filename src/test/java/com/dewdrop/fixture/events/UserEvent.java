package com.dewdrop.fixture.events;

import com.dewdrop.aggregate.AggregateId;
import com.dewdrop.read.readmodel.annotation.CreationEvent;
import com.dewdrop.structure.api.Event;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class UserEvent extends Event {
    @AggregateId
    private UUID userId;

    public UserEvent(UUID userId) {
        this.userId = userId;
    }
}
