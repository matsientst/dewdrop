package events.dewdrop.fixture.readmodel.accountdetails.details;

import events.dewdrop.api.result.Result;
import events.dewdrop.fixture.events.DewdropAccountCreated;
import events.dewdrop.read.readmodel.annotation.DewdropCache;
import events.dewdrop.read.readmodel.annotation.EventHandler;
import events.dewdrop.read.readmodel.annotation.ReadModel;
import events.dewdrop.read.readmodel.annotation.Stream;
import events.dewdrop.read.readmodel.query.QueryHandler;
import java.util.Map;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ReadModel(ephemeral = true, destroyInMinutesUnused = ReadModel.DESTROY_IMMEDIATELY)
@Stream(name = "DewdropAccountAggregate", subscribed = true)
@Stream(name = "DewdropUserAggregate", subscribed = false)
public class DewdropAccountDetailsReadModel {
    @DewdropCache
    Map<UUID, DewdropAccountDetails> cache;

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
