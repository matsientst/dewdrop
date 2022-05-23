package com.dewdrop.fixture;

import com.dewdrop.read.readmodel.annotation.CreationEvent;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@CreationEvent
public class UserCreated extends UserEvent {
    String username;

    public UserCreated(UUID userId, String username) {
        super(userId);
        this.username = username;
    }
}
