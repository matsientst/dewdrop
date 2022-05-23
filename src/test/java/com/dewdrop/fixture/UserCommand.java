package com.dewdrop.fixture;

import com.dewdrop.aggregate.AggregateId;
import com.dewdrop.structure.api.Command;
import java.util.UUID;
import lombok.Data;

@Data
public class UserCommand extends Command {
    @AggregateId
    private UUID userId;

    public UserCommand(UUID userId) {
        super();
        this.userId = userId;
    }
}
