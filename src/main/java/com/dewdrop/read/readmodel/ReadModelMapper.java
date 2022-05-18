package com.dewdrop.read.readmodel;

import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.serialize.EventSerializer;

public interface ReadModelMapper {

    CacheableCategoryReadModel<Message, Object> getReadModelByQuery(Object query);
    void init(StreamStore streamStore, EventSerializer eventSerializer);
}
