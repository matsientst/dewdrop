package events.dewdrop.fixture.events;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class DewdropUserDeactivate extends DewdropUserEvent {
    public DewdropUserDeactivate(UUID userId) {
        super(userId);
    }
}
