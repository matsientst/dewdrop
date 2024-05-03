package events.dewdrop.fixture.command.user;

import java.util.UUID;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserClaimUsernameCommand extends UserCommand {

    @NotBlank(message = "Username is required")
    String username;

    public UserClaimUsernameCommand(UUID userId, String username) {
        super(userId);
        this.username = username;
    }

}
