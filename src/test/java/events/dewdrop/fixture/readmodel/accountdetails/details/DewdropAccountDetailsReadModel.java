package events.dewdrop.fixture.readmodel.accountdetails.details;

import events.dewdrop.api.result.Result;
import events.dewdrop.fixture.events.DewdropAccountCreated;
import events.dewdrop.read.readmodel.annotation.CategoryStream;
import events.dewdrop.read.readmodel.annotation.DewdropCache;
import events.dewdrop.read.readmodel.annotation.EventHandler;
import events.dewdrop.read.readmodel.annotation.ReadModel;
import events.dewdrop.read.readmodel.query.QueryHandler;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.UUID;

@Log4j2
@ReadModel(destroyInMinutesUnused = ReadModel.DESTROY_IMMEDIATELY)
@CategoryStream(name = "DewdropAccountAggregate", subscribed = true)
@CategoryStream(name = "DewdropUserAggregate", subscribed = false)
public class DewdropAccountDetailsReadModel {
    @DewdropCache
    Map<UUID, DewdropAccountDetails> cache;

    @EventHandler
    public void on(DewdropAccountCreated event) {
        log.debug("This was called");
    }

    @QueryHandler
    public Result<DewdropAccountDetails> handle(DewdropGetAccountByIdQuery query) {
        log.info("Querying:{}, cache:{}", query, cache.values());
        DewdropAccountDetails dewdropAccountDetails = cache.get(query.getAccountId());
        log.info("dewdropAccountDetails: {}", dewdropAccountDetails);
        return (dewdropAccountDetails != null) ? Result.of(dewdropAccountDetails) : Result.empty();
    }
}
