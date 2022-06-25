package com.dewdrop.fixture.readmodel.users;

import com.dewdrop.fixture.events.DewdropUserCreated;
import com.dewdrop.read.readmodel.annotation.EventHandler;
import com.dewdrop.read.readmodel.annotation.PrimaryCacheKey;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Log4j2
public class DewdropUser {
    @PrimaryCacheKey
    private UUID userId;
    private String username;
    private Long version;

    @EventHandler
    public void on(DewdropUserCreated event) {
        this.userId = event.getUserId();
        this.username = event.getUsername();
    }

}
