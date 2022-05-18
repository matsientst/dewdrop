package com.dewdrop.fixture;

import com.dewdrop.api.result.Result;
import com.dewdrop.read.readmodel.ReadModel;
import com.dewdrop.read.readmodel.query.QueryHandler;
import java.util.Map;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ReadModel(rootEvent = DewdropAccountEvent.class, aggregateClass = DewdropAccountAggregate.class, resultClass = DewdropAccountDetails.class)
public class DewdropAccountDetailsReadModel {

    public void on(DewdropAccountCreated event, Map<UUID, DewdropAccountDetails> cachedItems) {
        log.info("on: {}", event);
    }

    @QueryHandler
    public Result<DewdropAccountDetails> handle(DewdropGetAccountByIdQuery query, Map<UUID, DewdropAccountDetails> cachedItems) {
        DewdropAccountDetails dewdropAccountDetails = cachedItems.get(query.getAccountId());
        if(dewdropAccountDetails != null) {
            return Result.of(dewdropAccountDetails);
        }
        return Result.empty();
    }
}
