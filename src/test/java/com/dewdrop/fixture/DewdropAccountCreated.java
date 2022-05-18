package com.dewdrop.fixture;

import com.dewdrop.read.readmodel.CacheRoot;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@CacheRoot
public class DewdropAccountCreated extends DewdropAccountEvent {
    private String name;

    public DewdropAccountCreated(UUID accountId, String name) {
        super(accountId);
        this.name = name;
    }
}
