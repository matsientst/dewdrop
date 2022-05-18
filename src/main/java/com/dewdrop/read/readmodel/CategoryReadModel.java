package com.dewdrop.read.readmodel;

import com.dewdrop.read.StreamDetails;
import com.dewdrop.read.StreamType;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.serialize.EventSerializer;
import java.util.function.Consumer;
import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class CategoryReadModel<T extends Message> extends AbstractReadModel<T> {
    private String category;

    protected CategoryReadModel(Class<?> messageType, StreamStore streamStoreConnection, EventSerializer eventSerializer) {
        super(messageType, StreamType.CATEGORY, streamStoreConnection, eventSerializer);
    }

    public String getCategory() {
        return category;
    }

    protected Consumer<Message> handler() {
        return message -> process((T) message);
    }

    abstract void process(T message);


    public void readAndSubscribe(String category, Consumer<Message> consumer, Class<?> messageType) {
        this.category = category;
        StreamDetails streamDetails = new StreamDetails(StreamType.CATEGORY, category);
        subscription.readAndSubscribe(streamDetails, consumer, messageType);
    }
}
