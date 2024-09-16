package events.dewdrop.fixture.readmodel.users;

import events.dewdrop.read.readmodel.annotation.AggregateStream;
import events.dewdrop.read.readmodel.annotation.DewdropCache;
import events.dewdrop.read.readmodel.annotation.ReadModel;
import events.dewdrop.read.readmodel.query.QueryHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ReadModel
@AggregateStream(name = "DewdropUserAggregate", subscribed = false)
public class DewdropUserAggregateReadModel {
    @DewdropCache
    DewdropUser dewdropUser;

    @QueryHandler
    public DewdropUser query(DewdropGetUserByIdQueryForAggregate userById) {
        log.info("Looking for userById:" + userById + ", dewdropUser:" + dewdropUser);
        return dewdropUser;
    }
}
