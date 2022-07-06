package org.dewdrop.fixture.command;

import java.util.UUID;
import lombok.Data;

@Data
public class DewdropCreateAccountCommand extends DewdropAccountCommand {
    private String name;
    private UUID userId;

    public DewdropCreateAccountCommand(UUID accountId, String name, UUID userId) {
        super(accountId);
        this.name = name;
        this.userId = userId;
    }
}
