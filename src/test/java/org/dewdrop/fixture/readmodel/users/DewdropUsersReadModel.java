package org.dewdrop.fixture.readmodel.users;

import org.dewdrop.read.readmodel.stream.StreamType;
import org.dewdrop.read.readmodel.annotation.DewdropCache;
import org.dewdrop.read.readmodel.annotation.ReadModel;
import org.dewdrop.read.readmodel.annotation.Stream;
import org.dewdrop.read.readmodel.query.QueryHandler;
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
    public DewdropUser query(GetUserByIdQuery userById) {
        DewdropUser dewdropUser = cache.get(userById.getUserId());
        return dewdropUser;
    }
}
