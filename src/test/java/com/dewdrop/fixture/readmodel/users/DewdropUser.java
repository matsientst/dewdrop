package com.dewdrop.fixture.readmodel.users;

import com.dewdrop.fixture.events.DewdropUserCreated;
import com.dewdrop.read.readmodel.annotation.EventHandler;
import com.dewdrop.read.readmodel.annotation.PrimaryCacheKey;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DewdropUser {
    @PrimaryCacheKey
    private UUID userId;
    private String username;

    @EventHandler
    public void on(DewdropUserCreated event) {
        this.userId = event.getUserId();
        this.username = event.getUsername();
    }

}
