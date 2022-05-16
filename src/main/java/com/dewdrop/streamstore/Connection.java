package com.dewdrop.streamstore;

import com.dewdrop.structure.StreamNameGenerator;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.serialize.EventSerializer;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Data
@Log4j2
@Component
public class Connection<T extends Message> {
    @Autowired
    private StreamStore streamStoreConnection;
    @Autowired
    private StreamNameGenerator streamNameGenerator;
    @Autowired
    private EventSerializer eventSerializer;

    public Connection(StreamStore streamStoreConnection, StreamNameGenerator streamNameGenerator, EventSerializer eventSerializer) {
        this.streamStoreConnection = streamStoreConnection;
        this.streamNameGenerator = streamNameGenerator;
        this.eventSerializer = eventSerializer;
    }
}
