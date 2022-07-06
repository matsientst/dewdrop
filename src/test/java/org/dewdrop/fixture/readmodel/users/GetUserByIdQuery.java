package org.dewdrop.fixture.readmodel.users;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetUserByIdQuery {
    private UUID userId;
}
