package com.dewdrop.read.readmodel;

import com.dewdrop.read.StreamType;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.read.Handler;
import com.dewdrop.structure.serialize.EventSerializer;
import com.dewdrop.streamstore.subscribe.Subscription;
import lombok.Data;

@Data
public abstract class AbstractReadModel<T extends Message> implements Handler<T> {
    protected Subscription<T> subscription;
    protected Class<?> messageType;
    protected StreamType streamType;

    protected AbstractReadModel(Class<?> messageType, StreamType streamType, StreamStore streamStoreConnection, EventSerializer eventSerializer) {
        this.messageType = messageType;
        this.streamType = streamType;
        this.subscription = new Subscription<>(this, messageType, streamStoreConnection, eventSerializer);
    }

    @Override
    public void handle(T event) {

    }

}
