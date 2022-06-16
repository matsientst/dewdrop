package com.dewdrop.read.readmodel;

import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.serialize.EventSerializer;

public interface ReadModelMapper {

    ReadModel<Event> getReadModelByQuery(Object query);

    void init(StreamStore streamStore, EventSerializer eventSerializer, StreamFactory streamFactory, ReadModelFactory readModelFactory);
}
