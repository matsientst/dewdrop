package org.dewdrop.fixture.readmodel;

import org.dewdrop.fixture.events.DewdropAccountCreated;
import org.dewdrop.read.readmodel.annotation.OnEvent;

public class AccountCreatedService {
    @OnEvent
    public void onAccountCreated(DewdropAccountCreated event) {
        System.out.println("AccountCreatedService.onAccountCreated");

    }
}
