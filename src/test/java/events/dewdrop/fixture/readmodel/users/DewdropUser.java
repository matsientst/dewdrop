package events.dewdrop.fixture.readmodel.users;

import events.dewdrop.read.readmodel.annotation.EventHandler;
import events.dewdrop.read.readmodel.annotation.PrimaryCacheKey;
import events.dewdrop.fixture.events.DewdropUserCreated;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Log4j2
public class DewdropUser {
    @PrimaryCacheKey
    private UUID userId;
    private String username;
    private Long version;

    @EventHandler
    private void on(DewdropUserCreated event) {
        this.userId = event.getUserId();
        this.username = event.getUsername();
    }

}
