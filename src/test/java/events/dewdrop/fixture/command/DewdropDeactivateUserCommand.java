package events.dewdrop.fixture.command;

import lombok.Data;

import java.util.UUID;

@Data
public class DewdropDeactivateUserCommand extends DewdropUserCommand {
    public DewdropDeactivateUserCommand(UUID userId) {
        super(userId);
    }
}
