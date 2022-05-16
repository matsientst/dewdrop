package com.dewdrop.structure.datastore;

import com.dewdrop.structure.NoStreamException;
import com.dewdrop.structure.events.StreamReadResults;
import com.dewdrop.structure.read.ReadRequest;
import com.dewdrop.structure.subscribe.SubscribeRequest;
import com.dewdrop.structure.write.WriteRequest;

public interface StreamStore {
    StreamReadResults read(ReadRequest readRequest) throws NoStreamException;

    void subscribeToStream(SubscribeRequest subscribeRequest) throws NoStreamException;

    void appendToStream(WriteRequest writeRequest);
}
