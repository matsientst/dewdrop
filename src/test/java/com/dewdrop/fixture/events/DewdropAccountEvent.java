package com.dewdrop.fixture.events;

import com.dewdrop.aggregate.annotation.AggregateId;
import com.dewdrop.structure.api.Event;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class DewdropAccountEvent extends Event {
    @AggregateId
    private UUID accountId;

}
