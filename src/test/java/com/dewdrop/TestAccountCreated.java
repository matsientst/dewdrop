package com.dewdrop;

import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TestAccountCreated extends TestAccountEvent {
    private String name;

    public TestAccountCreated(UUID accountId, String name) {
        super(accountId);
        this.name = name;
    }
}
