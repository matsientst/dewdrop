package events.dewdrop.fixture.readmodel.users.lifecycle;

import events.dewdrop.fixture.readmodel.users.GetUserByIdQuery;
import events.dewdrop.fixture.readmodel.users.User;
import events.dewdrop.read.readmodel.annotation.CategoryStream;
import events.dewdrop.read.readmodel.annotation.DewdropCache;
import events.dewdrop.read.readmodel.annotation.ReadModel;
import events.dewdrop.read.readmodel.query.QueryHandler;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.UUID;

@Log4j2
@ReadModel
@CategoryStream(name = "UserAggregate", subscribed = true)
@Getter
public class UsersReadModel {
    @DewdropCache
    Map<UUID, User> cache;

    @QueryHandler
    public User query(GetUserByIdQuery userById) {
        log.info("Looking for userById:" + userById + ", cacheSize:" + cache.size());

        User user = cache.get(userById.getUserId());
        return user;
    }
}
