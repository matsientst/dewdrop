package com.dewdropper;

import com.dewdropper.api.DefaultCommand;
import java.util.UUID;
import lombok.Data;

@Data
public class TestCreateAccountCommand extends TestAccountCommand {
    private String name;

    public TestCreateAccountCommand(UUID accountId, String name) {
        super(accountId);
        this.name = name;
    }
}
