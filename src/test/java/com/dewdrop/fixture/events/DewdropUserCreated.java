package com.dewdrop.fixture.events;

import com.dewdrop.read.readmodel.annotation.CreationEvent;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@CreationEvent
public class DewdropUserCreated extends DewdropUserEvent {
    String username;

    public DewdropUserCreated(UUID userId, String username) {
        super(userId);
        this.username = username;
    }
}
