package com.dewdrop.fixture.readmodel.users;

import com.dewdrop.read.StreamType;
import com.dewdrop.read.readmodel.annotation.DewdropCache;
import com.dewdrop.read.readmodel.annotation.ReadModel;
import com.dewdrop.read.readmodel.annotation.Stream;
import com.dewdrop.read.readmodel.query.QueryHandler;
import java.util.Map;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ReadModel(resultClass = DewdropUser.class)
@Stream(name = "DewdropUserCreated", streamType = StreamType.EVENT)
public class DewdropUsersReadModel {
    @DewdropCache
    Map<UUID, DewdropUser> cache;

    @QueryHandler
    public DewdropUser query(GetUserById userById) {
        DewdropUser dewdropUser = cache.get(userById.getUserId());
        return dewdropUser;
    }
}
