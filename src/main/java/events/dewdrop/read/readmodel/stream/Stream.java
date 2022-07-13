package events.dewdrop.read.readmodel.stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import events.dewdrop.aggregate.AggregateRoot;
import events.dewdrop.read.readmodel.stream.subscription.Subscription;
import events.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import events.dewdrop.streamstore.write.StreamWriter;
import events.dewdrop.structure.api.Event;
import events.dewdrop.structure.datastore.StreamStore;
import events.dewdrop.structure.read.Handler;
import events.dewdrop.structure.serialize.EventSerializer;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Data
@Log4j2
public class Stream<T extends Event> implements Handler<T> {
    private Subscription<T> subscription;
    StreamStore streamStore;
    EventSerializer eventSerializer;
    StreamDetails streamDetails;
    private AtomicLong streamPosition;

    public Stream(StreamDetails streamDetails, StreamStore streamStore, EventSerializer eventSerializer) {
        requireNonNull(streamDetails, "StreamDetails needed for a valid stream");
        requireNonNull(streamStore, "StreamStore needed for a valid stream");
        requireNonNull(eventSerializer, "EventSerializer needed for a valid stream");

        this.streamDetails = streamDetails;
        this.streamStore = streamStore;
        this.eventSerializer = eventSerializer;
        this.streamPosition = new AtomicLong(0L);
    }

    public void subscribe() {
        if (!streamDetails.isSubscribed()) { return; }
        log.debug("Creating Subscription for:{} - direction: {}, type: {}, messageType:{}", streamDetails.getStreamName(), streamDetails.getDirection(), streamDetails.getStreamType(),
                        streamDetails.getMessageTypes().stream().map(Class::getSimpleName).collect(joining(",")));
        subscription = Subscription.getInstance(this);
        StreamReader streamReader = StreamReader.getInstance(streamStore, eventSerializer, streamDetails);

        if (!subscription.subscribeByNameAndPosition(streamReader)) {
            log.info("Unable to find stream:{} will poll until we find then subscribe", streamDetails.getStreamName());
            subscription.pollForCompletion(streamReader);
            return;
        }
    }

    public void read(Long start, Long count) {
        StreamReader streamReader = StreamReader.getInstance(streamStore, eventSerializer, streamDetails, streamPosition);
        streamReader.read(start, count);
        this.streamPosition = streamReader.getStreamPosition();
    }

    @Override
    public void handle(T event) {
        streamDetails.getEventHandler().accept(event);
    }

    // If we don't have a subscription we can call read to catch up to where we need to be in our
    // version
    public void updateState() {
        if (!streamDetails.isSubscribed()) {
            this.read(this.streamPosition.get(), null);
        }
    }

    public AggregateRoot getById(StreamStoreGetByIDRequest request) {
        requireNonNull(request, "A StreamStoreGetByIDRequest is required");

        if (streamDetails.getStreamType() != StreamType.AGGREGATE) { throw new IllegalStateException("Stream is not an aggregate - we cannot get by id"); }

        StreamReader streamReader = StreamReader.getInstance(streamStore, eventSerializer, streamDetails);
        return streamReader.getById(request);
    }

    public void save(AggregateRoot aggregateRoot) {
        StreamWriter streamWriter = StreamWriter.getInstance(streamDetails, streamStore, eventSerializer);
        streamWriter.save(aggregateRoot);
    }
}
