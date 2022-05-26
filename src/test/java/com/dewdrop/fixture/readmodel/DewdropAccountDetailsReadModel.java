package com.dewdrop.fixture.readmodel;

import com.dewdrop.api.result.Result;
import com.dewdrop.fixture.events.DewdropAccountCreated;
import com.dewdrop.fixture.events.DewdropAccountEvent;
import com.dewdrop.fixture.events.UserEvent;
import com.dewdrop.read.readmodel.annotation.ReadModel;
import com.dewdrop.read.readmodel.annotation.Stream;
import com.dewdrop.read.readmodel.query.QueryHandler;
import java.util.Map;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ReadModel(resultClass = DewdropAccountDetails.class)
@Stream(name = "DewdropAccountAggregate", rootEvent = DewdropAccountEvent.class)
@Stream(name = "DewdropUserAggregate", rootEvent = UserEvent.class, subscribed = false)
public class DewdropAccountDetailsReadModel {

    public void on(DewdropAccountCreated event, Map<UUID, DewdropAccountDetails> cachedItems) {

    }

    @QueryHandler
    public Result<DewdropAccountDetails> handle(DewdropGetAccountByIdQuery query, Map<UUID, DewdropAccountDetails> cachedItems) {
        DewdropAccountDetails dewdropAccountDetails = cachedItems.get(query.getAccountId());
        if (dewdropAccountDetails != null) { return Result.of(dewdropAccountDetails); }
        return Result.empty();
    }
}
