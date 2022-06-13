package com.dewdrop.read.readmodel.stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.read.StreamDetails;
import com.dewdrop.read.StreamReader;
import com.dewdrop.read.StreamType;
import com.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import com.dewdrop.streamstore.subscribe.StreamListener;
import com.dewdrop.streamstore.subscribe.Subscription;
import com.dewdrop.streamstore.write.StreamWriter;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.read.Handler;
import com.dewdrop.structure.serialize.EventSerializer;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Data
@Log4j2
public class Stream<T extends Message> implements Handler<T> {
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
        log.info("Creating Stream for stream:{} - subscribed:{}", streamDetails.getStreamName(), streamDetails.isSubscribed());
    }

    public void subscribe() {
        if (!streamDetails.isSubscribed()) { return; }
        log.debug("Creating Subscription for:{} - direction: {}, type: {}, messageType:{}, from position:{}", streamDetails.getStreamName(), streamDetails.getDirection(), streamDetails.getStreamType(),
                        streamDetails.getMessageTypes().stream().map(Class::getSimpleName).collect(joining(",")));
        subscription = new Subscription<>(this, streamDetails.getMessageTypes(), new StreamListener<>(streamStore, eventSerializer));
        StreamReader streamReader = new StreamReader(streamStore, eventSerializer, streamDetails);

        if (!subscription.subscribeByNameAndPosition(streamReader)) {
            log.info("Unable to find stream:{} will poll until we find then subscribe", streamDetails.getStreamName());
            subscription.pollForCompletion(streamReader);
            return;
        }
    }

    public void read(Long start, Long count) {
        StreamReader streamReader = new StreamReader(streamStore, eventSerializer, streamDetails, streamPosition);
        streamReader.read(start, count);
        this.streamPosition = streamReader.getStreamPosition();
    }

    @Override
    public void handle(T event) {
        streamDetails.getEventHandler().accept(event);
    }

    public List<Class<?>> getMessageTypes() {
        return streamDetails.getMessageTypes();
    }

    // If we don't have a subscription we can call read to catch up to where we need to be in our
    // version
    public void updateState() {
        if (!streamDetails.isSubscribed()) {
            this.read(this.streamPosition.get(), null);
        }
    }

    public AggregateRoot getById(StreamStoreGetByIDRequest request) {
        if (streamDetails.getStreamType() != StreamType.AGGREGATE) { throw new IllegalStateException("Stream is not an aggregate"); }

        StreamReader streamReader = new StreamReader(streamStore, eventSerializer, streamDetails);
        return streamReader.getById(request);
    }

    public void save(AggregateRoot aggregateRoot) {
        StreamWriter streamWriter = new StreamWriter(streamDetails, streamStore, eventSerializer);
        streamWriter.save(aggregateRoot);
    }
}
