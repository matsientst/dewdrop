package events.dewdrop.fixture.readmodel;

import events.dewdrop.read.readmodel.annotation.OnEvent;
import events.dewdrop.fixture.events.DewdropAccountCreated;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AccountCreatedService {
    @OnEvent
    public void onAccountCreated(DewdropAccountCreated event) {
        log.info("AccountCreatedService.onAccountCreated");

    }
}
