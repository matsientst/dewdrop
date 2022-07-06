package org.dewdrop.structure.datastore;

import org.dewdrop.structure.NoStreamException;
import org.dewdrop.structure.events.StreamReadResults;
import org.dewdrop.structure.read.ReadRequest;
import org.dewdrop.structure.subscribe.SubscribeRequest;
import org.dewdrop.structure.write.WriteRequest;

public interface StreamStore {
    StreamReadResults read(ReadRequest readRequest) throws NoStreamException;

    boolean subscribeToStream(SubscribeRequest subscribeRequest) throws NoStreamException;

    void appendToStream(WriteRequest writeRequest);
}
