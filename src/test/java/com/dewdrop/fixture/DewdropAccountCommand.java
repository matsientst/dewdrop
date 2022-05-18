package com.dewdrop.fixture;

import com.dewdrop.aggregate.AggregateId;
import com.dewdrop.api.DefaultCommand;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DewdropAccountCommand extends DefaultCommand {
    @AggregateId
    private UUID accountId;
}
