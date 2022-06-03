package com.dewdrop.fixture.command;

import java.util.UUID;
import lombok.Data;

@Data
public class DewdropCreateUserCommand extends DewdropUserCommand {
    String username;

    public DewdropCreateUserCommand(UUID userId, String username) {
        super(userId);
        this.username = username;
    }
}
