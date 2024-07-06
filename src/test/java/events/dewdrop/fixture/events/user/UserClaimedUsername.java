package events.dewdrop.fixture.events.user;

import events.dewdrop.read.readmodel.annotation.CreationEvent;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserClaimedUsername extends UserEvent {
    String username;

    public UserClaimedUsername(UUID userId, String username) {
        super(userId);
        this.username = username;
    }
}
