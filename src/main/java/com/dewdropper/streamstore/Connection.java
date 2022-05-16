package com.dewdropper.streamstore;

import com.dewdropper.structure.StreamNameGenerator;
import com.dewdropper.structure.datastore.StreamStore;
import com.dewdropper.structure.api.Message;
import com.dewdropper.structure.serialize.EventSerializer;
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
