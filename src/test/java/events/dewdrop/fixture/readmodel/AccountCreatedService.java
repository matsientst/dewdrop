package events.dewdrop.fixture.readmodel;

import events.dewdrop.fixture.events.DewdropAccountCreated;
import events.dewdrop.read.readmodel.annotation.OnEvent;
import events.dewdrop.read.readmodel.annotation.StreamStartPosition;
import events.dewdrop.read.readmodel.stream.StreamType;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AccountCreatedService {
    @OnEvent
    public void onAccountCreated(DewdropAccountCreated event) {
        log.info("AccountCreatedService.onAccountCreated");
    }

    @StreamStartPosition(name = "DewdropAccountCreated", streamType = StreamType.EVENT)
    public long startPosition() {
        return 0;
    }
}
