package com.dewdrop.fixture.events;

import com.dewdrop.read.readmodel.annotation.CreationEvent;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@CreationEvent
public class DewdropAccountCreated extends DewdropAccountEvent {
    private String name;
    private UUID userId;

    public DewdropAccountCreated(UUID accountId, String name, UUID userId) {
        super(accountId);
        this.name = name;
        this.userId = userId;
    }
}
