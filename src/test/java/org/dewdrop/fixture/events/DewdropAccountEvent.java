package org.dewdrop.fixture.events;

import org.dewdrop.aggregate.annotation.AggregateId;
import org.dewdrop.structure.api.Event;
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
