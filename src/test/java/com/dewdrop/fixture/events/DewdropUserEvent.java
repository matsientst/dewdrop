package com.dewdrop.fixture.events;

import com.dewdrop.aggregate.annotation.AggregateId;
import com.dewdrop.structure.api.Event;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "userId")
public abstract class DewdropUserEvent extends Event {
    @AggregateId
    private UUID userId;

    public DewdropUserEvent(UUID userId) {
        this.userId = userId;
    }
}
