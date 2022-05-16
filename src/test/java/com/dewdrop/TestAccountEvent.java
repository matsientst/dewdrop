package com.dewdrop;

import com.dewdrop.aggregate.AggregateId;
import com.dewdrop.structure.api.Event;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestAccountEvent extends Event {
    @AggregateId
    private UUID accountId;

}
