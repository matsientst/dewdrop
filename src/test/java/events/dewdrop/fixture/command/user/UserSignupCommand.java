package events.dewdrop.fixture.command.user;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserSignupCommand extends UserCommand {
    @NotBlank(message = "Email is required")
    String email;

    public UserSignupCommand(UUID userId, String email) {
        super(userId);
        this.email = email;
    }
}
