package com.dewdrop.fixture.events;

import com.dewdrop.aggregate.annotation.AggregateId;
import com.dewdrop.structure.api.Event;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class DewdropUserEvent extends Event {
    @AggregateId
    private UUID userId;

    public DewdropUserEvent(UUID userId) {
        this.userId = userId;
    }
}
