package com.dewdrop.fixture;

import java.util.UUID;
import lombok.Data;

@Data
public class DewdropCreateAccountCommand extends DewdropAccountCommand {
    private String name;

    public DewdropCreateAccountCommand(UUID accountId, String name) {
        super(accountId);
        this.name = name;
    }
}
