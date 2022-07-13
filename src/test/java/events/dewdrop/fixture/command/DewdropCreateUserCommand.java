package events.dewdrop.fixture.command;

import java.util.UUID;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DewdropCreateUserCommand extends DewdropUserCommand {
    @NotBlank(message = "Username is required")
    String username;

    public DewdropCreateUserCommand(UUID userId, String username) {
        super(userId);
        this.username = username;
    }
}
