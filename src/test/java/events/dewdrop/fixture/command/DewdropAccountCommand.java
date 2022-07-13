package events.dewdrop.fixture.command;

import events.dewdrop.aggregate.annotation.AggregateId;
import events.dewdrop.structure.api.Command;
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
