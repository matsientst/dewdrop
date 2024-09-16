package events.dewdrop.fixture.readmodel.users;

import events.dewdrop.aggregate.annotation.AggregateId;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class DewdropGetUserByIdQueryForAggregate {
    @AggregateId
    private UUID userId;
}
