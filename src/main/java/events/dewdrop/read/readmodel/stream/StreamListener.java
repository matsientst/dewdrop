package events.dewdrop.read.readmodel.stream;

import events.dewdrop.read.readmodel.stream.subscription.Subscription;
import events.dewdrop.structure.NoStreamException;
import events.dewdrop.structure.datastore.StreamStore;
import events.dewdrop.structure.events.ReadEventData;
import events.dewdrop.structure.serialize.EventSerializer;
import events.dewdrop.structure.subscribe.SubscribeRequest;
import events.dewdrop.structure.api.Event;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class StreamListener<T extends Event> {
    private StreamStore streamStore;
    private EventSerializer serializer;
    private String streamName;
    private AtomicLong streamPosition;

    private StreamListener(StreamStore streamStore, EventSerializer serializer) {
        this.streamStore = streamStore;
        this.serializer = serializer;
        this.streamPosition = new AtomicLong(0L);
    }

    public static StreamListener getInstance(StreamStore streamStore, EventSerializer serializer) {
        return new StreamListener(streamStore, serializer);
    }

    public boolean start(String streamName, Long checkpoint, Subscription subscription) throws NoStreamException {
        this.streamName = streamName;
        return subscribe(checkpoint, onEvent(subscription));
    }

    protected Consumer<ReadEventData> onEvent(Subscription<T> subscription) {
        return readEventData -> {
            Optional<T> deserializedEvent = serializer.deserialize(readEventData);
            if (deserializedEvent.isPresent()) {
                subscription.publish(deserializedEvent.get());
                streamPosition.setRelease(readEventData.getEventNumber());
                return;
            } else {
                log.error("Failed to deserialize event:" + readEventData.getEventType());
            }
        };
    }

    boolean subscribe(Long lastCheckpoint, Consumer<ReadEventData> eventHandler) throws NoStreamException {
        SubscribeRequest subscribeRequest = new SubscribeRequest(streamName, lastCheckpoint, eventHandler);
        return streamStore.subscribeToStream(subscribeRequest);
    }
}
