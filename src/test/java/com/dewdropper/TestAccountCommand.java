package com.dewdropper;

import com.dewdropper.aggregate.AggregateId;
import com.dewdropper.api.DefaultCommand;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TestAccountCommand extends DefaultCommand {
    @AggregateId
    private UUID accountId;
}
