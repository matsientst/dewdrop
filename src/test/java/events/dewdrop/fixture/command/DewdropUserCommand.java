package events.dewdrop.fixture.command;

import events.dewdrop.aggregate.annotation.AggregateId;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import events.dewdrop.structure.api.Command;

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
