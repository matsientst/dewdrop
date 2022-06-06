package com.dewdrop.fixture.command;

import com.dewdrop.aggregate.annotation.AggregateId;
import com.dewdrop.structure.api.Command;
import java.util.UUID;
import lombok.Data;

@Data
public class DewdropUserCommand extends Command {
    @AggregateId
    private UUID userId;

    public DewdropUserCommand(UUID userId) {
        super();
        this.userId = userId;
    }
}
