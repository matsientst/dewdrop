package events.dewdrop.fixture.readmodel.users;

import events.dewdrop.fixture.events.user.UserClaimedUsername;
import events.dewdrop.fixture.events.user.UserSignedUp;
import events.dewdrop.read.readmodel.annotation.EventHandler;
import events.dewdrop.read.readmodel.annotation.PrimaryCacheKey;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Log4j2
public class User {
    @PrimaryCacheKey
    private UUID userId;
    private String username;
    private String email;
    private Long version;

    @EventHandler
    private void on(UserSignedUp event) {
        log.info("Processing UserSignedUp:{}", event);
        this.userId = event.getUserId();
        this.email = event.getEmail();
    }

    @EventHandler
    private void on(UserClaimedUsername event) {

        log.info("Processing UserClaimedUsername:{}", event);
        this.username = event.getUsername();
    }

}
