package org.dewdrop.fixture.command;

import org.dewdrop.aggregate.annotation.AggregateId;
import org.dewdrop.structure.api.Command;
import java.util.UUID;
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
