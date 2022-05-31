package com.dewdrop.fixture.command;

import com.dewdrop.aggregate.AggregateId;
import com.dewdrop.structure.api.Command;
import com.dewdrop.structure.api.Message;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data

public abstract class DewdropAccountCommand extends Command {
    @AggregateId
    private UUID accountId;

    public DewdropAccountCommand(UUID accountId) {
        super();
        this.accountId = accountId;
    }
}
