package com.dewdrop.fixture.readmodel.accountdetails.details;

import com.dewdrop.api.result.Result;
import com.dewdrop.fixture.events.DewdropAccountCreated;
import com.dewdrop.fixture.readmodel.users.DewdropUser;
import com.dewdrop.read.readmodel.annotation.DewdropCache;
import com.dewdrop.read.readmodel.annotation.EventHandler;
import com.dewdrop.read.readmodel.annotation.ReadModel;
import com.dewdrop.read.readmodel.annotation.Stream;
import com.dewdrop.read.readmodel.query.QueryHandler;
import java.util.Map;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ReadModel(resultClass = DewdropAccountDetails.class)
@Stream(name = "DewdropAccountAggregate")
@Stream(name = "DewdropUserAggregate", subscribed = false)
public class DewdropAccountDetailsReadModel {
    @DewdropCache
    Map<UUID, DewdropAccountDetails> cache;
    // list of users - listen to the usercreated event type, user deleted or user disabled
    // accounts per user - listen to account created user assignment (create two events in aggregate) -
    // listen for events
    // account detail - aggregate stream account-id stream - Need to have this be constructed real time
    // as we need it.

    @EventHandler
    public void on(DewdropAccountCreated event, Map<UUID, DewdropAccountDetails> cachedItems) {
        log.debug("This was called");
    }

    @QueryHandler
    public Result<DewdropAccountDetails> handle(DewdropGetAccountByIdQuery query) {
        DewdropAccountDetails dewdropAccountDetails = cache.get(query.getAccountId());
        if (dewdropAccountDetails != null) { return Result.of(dewdropAccountDetails); }
        return Result.empty();
    }
}
