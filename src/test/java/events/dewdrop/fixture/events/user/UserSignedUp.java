package events.dewdrop.fixture.events.user;

import events.dewdrop.read.readmodel.annotation.CreationEvent;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@CreationEvent
@ToString(callSuper = true)
public class UserSignedUp extends UserEvent {
    String email;

    public UserSignedUp(UUID userId, String email) {
        super(userId);
        this.email = email;
    }
}
