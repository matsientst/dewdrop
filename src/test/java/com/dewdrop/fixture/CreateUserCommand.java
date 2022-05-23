package com.dewdrop.fixture;

import java.util.UUID;
import lombok.Data;

@Data
public class CreateUserCommand extends UserCommand {
    String username;

    public CreateUserCommand(UUID userId, String username) {
        super(userId);
        this.username = username;
    }
}
