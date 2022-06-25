package com.dewdrop.fixture.events;

import com.dewdrop.aggregate.annotation.AggregateId;
import com.dewdrop.structure.api.Event;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "accountId")
public abstract class DewdropAccountEvent extends Event {
    @AggregateId
    private UUID accountId;

}
