package events.dewdrop.fixture.readmodel.users;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DewdropGetUserByIdQuery {
    private UUID userId;
}
