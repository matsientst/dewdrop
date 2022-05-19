package com.dewdrop.fixture;

import com.dewdrop.read.readmodel.CreationEvent;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@CreationEvent
public class DewdropAccountCreated extends DewdropAccountEvent {
    private String name;

    public DewdropAccountCreated(UUID accountId, String name) {
        super(accountId);
        this.name = name;
    }
}
