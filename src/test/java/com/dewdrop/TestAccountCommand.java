package com.dewdrop;

import com.dewdrop.aggregate.AggregateId;
import com.dewdrop.api.DefaultCommand;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TestAccountCommand extends DefaultCommand {
    @AggregateId
    private UUID accountId;
}
