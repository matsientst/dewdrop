package events.dewdrop.structure.datastore;

import events.dewdrop.structure.NoStreamException;
import events.dewdrop.structure.events.StreamReadResults;
import events.dewdrop.structure.read.ReadRequest;
import events.dewdrop.structure.subscribe.SubscribeRequest;
import events.dewdrop.structure.write.WriteRequest;

public interface StreamStore {
    StreamReadResults read(ReadRequest readRequest) throws NoStreamException;

    boolean subscribeToStream(SubscribeRequest subscribeRequest) throws NoStreamException;

    void appendToStream(WriteRequest writeRequest);
}
