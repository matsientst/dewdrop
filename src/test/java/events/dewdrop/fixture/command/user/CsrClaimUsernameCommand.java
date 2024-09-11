package events.dewdrop.fixture.command.user;

import java.util.UUID;

import lombok.Data;

@Data
public class CsrClaimUsernameCommand extends UserClaimUsernameCommand {

    public CsrClaimUsernameCommand(UUID userId, String username) {
        super(userId, username);
    }

}
