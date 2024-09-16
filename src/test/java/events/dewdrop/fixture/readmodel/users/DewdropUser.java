package events.dewdrop.fixture.readmodel.users;

import events.dewdrop.fixture.events.DewdropUserCreated;
import events.dewdrop.fixture.events.DewdropUserDeactivate;
import events.dewdrop.read.readmodel.annotation.EventHandler;
import events.dewdrop.read.readmodel.annotation.PrimaryCacheKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Log4j2
public class DewdropUser {
    @PrimaryCacheKey(creationEvent = DewdropUserCreated.class)
    private UUID userId;
    private String username;
    private Long version;
    private boolean active = true;

    @EventHandler
    private void on(DewdropUserCreated event) {
        log.info("Processing DewdropUserCreated,{}", event);
        this.userId = event.getUserId();
        this.username = event.getUsername();
    }

    @EventHandler
    private void on(DewdropUserDeactivate event) {
        this.active = false;
    }

}
