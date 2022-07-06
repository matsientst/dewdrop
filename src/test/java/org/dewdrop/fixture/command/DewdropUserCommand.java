package org.dewdrop.fixture.command;

import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dewdrop.aggregate.annotation.AggregateId;
import org.dewdrop.structure.api.Command;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DewdropUserCommand extends Command {
    @NotNull(message = "UserId is required")
    @AggregateId
    private UUID userId;

    public DewdropUserCommand(UUID userId) {
        super();
        this.userId = userId;
    }
}
