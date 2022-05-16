package com.dewdrop.streamstore.subscribe;

import com.dewdrop.structure.NoStreamException;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.events.ReadEventData;
import com.dewdrop.structure.read.Direction;
import com.dewdrop.structure.read.ReadRequest;
import com.dewdrop.structure.serialize.EventSerializer;
import com.dewdrop.structure.subscribe.SubscribeRequest;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class StreamListener<T extends Message> {
    private StreamStore streamStoreConnection;
    private EventSerializer serializer;
    private String streamName;
    private AtomicLong streamPosition;
    ScheduledExecutorService es;

    public StreamListener(Class<?> eventType, StreamStore streamStoreConnection, EventSerializer serializer) {
        this.streamStoreConnection = streamStoreConnection;
        this.serializer = serializer;
        this.streamPosition = new AtomicLong(0L);
    }

    public void start(String streamName, Long checkpoint, Subscription subscription) throws NoStreamException {
        this.streamName = streamName;
        subscribe(checkpoint, onEvent(subscription));
    }


    protected Consumer<ReadEventData> onEvent(Subscription<T> subscription) {
        return readEventData -> {
            try {
                streamPosition.setRelease(readEventData.getEventNumber());
                Optional<T> deserializedEvent = serializer.deserialize(readEventData);
                if (deserializedEvent.isPresent()) {
                    subscription.publish(deserializedEvent.get());
                    return;
                }
                log.error("Failed to deserialize event:" + readEventData.getEventType());
            } catch (Exception e) {
                log.error("Problem deserializing readEventData", e);
            }

        };
    }


    public boolean validateStreamName(String streamName) {
        try {
            ReadRequest readRequest = new ReadRequest(streamName, 0L, 1L, Direction.FORWARD);
            return streamStoreConnection.read(readRequest) != null;
        } catch (NoStreamException e) {
            return false;
        }
    }


    private void subscribe(Long lastCheckpoint, Consumer<ReadEventData> eventHandler) throws NoStreamException {
        SubscribeRequest subscribeRequest = new SubscribeRequest(streamName, lastCheckpoint, eventHandler);
        streamStoreConnection.subscribeToStream(subscribeRequest);
    }
}
