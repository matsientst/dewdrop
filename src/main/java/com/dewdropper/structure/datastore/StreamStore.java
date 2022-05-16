package com.dewdropper.structure.datastore;

import com.dewdropper.structure.NoStreamException;
import com.dewdropper.structure.events.StreamReadResults;
import com.dewdropper.structure.read.ReadRequest;
import com.dewdropper.structure.subscribe.SubscribeRequest;
import com.dewdropper.structure.write.WriteRequest;

public interface StreamStore {
    StreamReadResults read(ReadRequest readRequest) throws NoStreamException;

    void subscribeToStream(SubscribeRequest subscribeRequest) throws NoStreamException;

    void appendToStream(WriteRequest writeRequest);
}
