package com.dewdrop.fixture.readmodel.users;

import com.dewdrop.read.StreamType;
import com.dewdrop.read.readmodel.annotation.DewdropCache;
import com.dewdrop.read.readmodel.annotation.ReadModel;
import com.dewdrop.read.readmodel.annotation.Stream;
import com.dewdrop.read.readmodel.query.QueryHandler;
import java.util.List;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ReadModel(resultClass = DewdropUser.class)
@Stream(name = "DewdropUserCreated", streamType = StreamType.EVENT)
@Getter
public class DewdropUsersReadModel {
    @DewdropCache
    List<DewdropUser> cache;

    @QueryHandler
    public DewdropUser query(GetUserByIdQuery userById) {
        DewdropUser dewdropUser = cache.stream().filter(user -> user.getUserId().equals(userById.getUserId())).findAny().orElse(null);
        return dewdropUser;
    }
}
