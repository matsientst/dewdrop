package events.dewdrop.fixture.readmodel.users;

import events.dewdrop.read.readmodel.annotation.DewdropCache;
import events.dewdrop.read.readmodel.annotation.EventStream;
import events.dewdrop.read.readmodel.annotation.ReadModel;
import events.dewdrop.read.readmodel.query.QueryHandler;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.UUID;

@Log4j2
@ReadModel
@EventStream(name = "DewdropUserCreated")
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
