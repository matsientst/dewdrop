package events.dewdrop.fixture.events.user;

import events.dewdrop.read.readmodel.annotation.CreationEvent;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@CreationEvent
public class CsrClaimedUsername extends UserClaimedUsername {

    public CsrClaimedUsername(UUID userId, String username) {
        super(userId, username);
    }
}
