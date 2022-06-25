package com.dewdrop.fixture.readmodel;

import com.dewdrop.fixture.events.DewdropAccountCreated;
import com.dewdrop.read.readmodel.annotation.OnEvent;

public class AccountCreatedService {
    @OnEvent
    public void onAccountCreated(DewdropAccountCreated event) {

        System.out.println("AccountCreatedService.onAccountCreated");
    }
}
