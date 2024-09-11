package events.dewdrop.fixture.readmodel.users;

import events.dewdrop.read.readmodel.annotation.DewdropCache;
import events.dewdrop.read.readmodel.annotation.ReadModel;
import events.dewdrop.read.readmodel.annotation.Stream;
import events.dewdrop.read.readmodel.query.QueryHandler;
import events.dewdrop.read.readmodel.stream.StreamType;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ReadModel
@Stream(name = "DewdropUserCreated", streamType = StreamType.EVENT)
@Getter
public class DewdropUsersReadModel {
    @DewdropCache
    Map<UUID, DewdropUser> cache;

    @QueryHandler
    public DewdropUser query(DewdropGetUserByIdQuery userById) {
        log.info("Looking for userById:" + userById + ", cache:" + cache.values());

        DewdropUser dewdropUser = cache.get(userById.getUserId());
        return dewdropUser;
    }
}
