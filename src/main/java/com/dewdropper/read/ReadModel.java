package com.dewdropper.read;

import com.dewdropper.structure.datastore.StreamStore;
import com.dewdropper.structure.api.Message;
import com.dewdropper.structure.read.Handler;
import com.dewdropper.structure.serialize.EventSerializer;
import com.dewdropper.streamstore.subscribe.Subscription;
import lombok.Data;

@Data
public abstract class ReadModel<T extends Message> implements Handler<T> {
    protected Subscription<T> subscription;
    protected Class<?> messageType;
    protected StreamType streamType;

    protected ReadModel(Class<?> messageType, StreamType streamType, StreamStore streamStoreConnection, EventSerializer eventSerializer) {
        this.messageType = messageType;
        this.streamType = streamType;
        this.subscription = new Subscription<>(this, messageType, streamStoreConnection, eventSerializer);
    }

    @Override
    public void handle(T event) {

    }

}
