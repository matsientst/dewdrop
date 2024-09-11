package events.dewdrop.fixture.command.user;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserClaimUsernameCommand extends UserCommand {

    @NotBlank(message = "Username is required")
    String username;

    public UserClaimUsernameCommand(UUID userId, String username) {
        super(userId);
        this.username = username;
    }

}
