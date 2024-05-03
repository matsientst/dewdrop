package events.dewdrop.fixture.command.user;

import events.dewdrop.aggregate.annotation.AggregateId;
import events.dewdrop.structure.api.Command;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserCommand extends Command {

    @NotNull(message = "UserId is required")
    @AggregateId
    private UUID userId;

    public UserCommand(UUID userId) {
        super();
        this.userId = userId;
    }
}
