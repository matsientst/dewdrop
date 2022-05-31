package com.dewdrop.fixture.readmodel.users;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetUserById {
    private UUID userId;
}
