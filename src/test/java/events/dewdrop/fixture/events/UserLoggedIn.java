package events.dewdrop.fixture.events;

import java.time.LocalDateTime;
import java.util.UUID;

import events.dewdrop.fixture.events.user.UserEvent;
import lombok.Data;

@Data
public class UserLoggedIn extends UserEvent {
    private LocalDateTime login;

    public UserLoggedIn(UUID userId, LocalDateTime login) {
        super(userId);
        this.login = login;
    }
}
