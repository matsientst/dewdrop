package events.dewdrop.fixture.readmodel;

import events.dewdrop.read.readmodel.annotation.OnEvent;
import events.dewdrop.fixture.events.DewdropAccountCreated;

public class AccountCreatedService {
    @OnEvent
    public void onAccountCreated(DewdropAccountCreated event) {
        System.out.println("AccountCreatedService.onAccountCreated");

    }
}
