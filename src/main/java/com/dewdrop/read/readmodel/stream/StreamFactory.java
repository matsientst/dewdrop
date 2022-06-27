package com.dewdrop.read.readmodel.stream;

import static java.util.Objects.requireNonNull;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.read.readmodel.annotation.Stream;
import com.dewdrop.structure.StreamNameGenerator;
import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.read.Direction;
import com.dewdrop.structure.serialize.EventSerializer;
import com.dewdrop.utils.AggregateIdUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * It's a factory that creates streams
 */
public class StreamFactory {
    private StreamNameGenerator streamNameGenerator;
    private StreamStore streamStore;
    private EventSerializer eventSerializer;


    public StreamFactory(StreamStore streamStore, EventSerializer eventSerializer, StreamNameGenerator streamNameGenerator) {
        requireNonNull(streamStore, "streamStore is required");
        requireNonNull(eventSerializer, "eventSerializer is required");
        requireNonNull(streamNameGenerator, "StreamNameGenerator is required");

        this.streamStore = streamStore;
        this.eventSerializer = eventSerializer;
        this.streamNameGenerator = streamNameGenerator;
    }

    /**
     * Construct a StreamDetails from a stream annotation, an event handler, and a list of message types
     *
     * @param streamAnnotation The annotation that was placed on the method
     * @param eventHandler The method that will be called when an event is received.
     * @param messageTypes The list of event types that the event handler can handle.
     * @return StreamDetails
     */
    private StreamDetails fromStreamAnnotation(Stream streamAnnotation, Consumer<Event> eventHandler, List<Class<? extends Event>> messageTypes) {
        requireNonNull(streamAnnotation, "StreamAnnotation is required");
        requireNonNull(eventHandler, "EventHandler is required");

        return StreamDetails.builder().streamType(streamAnnotation.streamType()).direction(streamAnnotation.direction()).eventHandler(eventHandler).streamNameGenerator(streamNameGenerator).messageTypes(messageTypes).name(streamAnnotation.name())
                        .subscribed(streamAnnotation.subscribed()).create();
    }

    /**
     * It creates a `StreamDetails` object from an `AggregateRoot` object, and a UUID
     *
     * @param aggregateRoot The aggregate root to be serialized.
     * @param overrideId This is the id of the aggregate root. If you don't provide it, the id will be
     *        extracted from the aggregate root.
     * @return A StreamDetails object
     */
    private StreamDetails fromAggregateRoot(final AggregateRoot aggregateRoot, final UUID overrideId) {
        UUID id = Optional.ofNullable(overrideId).orElseGet(() -> AggregateIdUtils.getAggregateId(aggregateRoot).orElse(null));
        requireNonNull(id, "aggregateId is required");

        return StreamDetails.builder().streamType(StreamType.AGGREGATE).direction(Direction.FORWARD).aggregateRoot(aggregateRoot).streamNameGenerator(streamNameGenerator).id(id).create();
    }

    /**
     * It creates a StreamDetails object that is used to create a stream that subscribes to an event
     * stream
     *
     * @param eventConsumer The consumer of the event.
     * @param eventClass The class of the event you want to listen to.
     * @return A StreamDetails object.
     */
    private <T extends Event> StreamDetails fromEvent(Consumer<Event> eventConsumer, Class<? extends Event> eventClass) {
        return StreamDetails.builder().streamType(StreamType.EVENT).direction(Direction.FORWARD).eventHandler(eventConsumer).streamNameGenerator(streamNameGenerator).messageTypes(List.of(eventClass)).name(eventClass.getSimpleName()).subscribed(true)
                        .subscriptionStartStrategy(SubscriptionStartStrategy.START_END_ONLY).create();
    }

    /**
     * Construct a Stram from an AggregateRoot object and a UUID
     *
     * @param aggregateRoot The aggregate root that you want to construct a stream for.
     * @param aggregateRootId The id of the aggregate root.
     * @return A Stream object.
     */
    public com.dewdrop.read.readmodel.stream.Stream constructStream(AggregateRoot aggregateRoot, UUID aggregateRootId) {
        StreamDetails streamDetails = fromAggregateRoot(aggregateRoot, aggregateRootId);
        com.dewdrop.read.readmodel.stream.Stream stream = new com.dewdrop.read.readmodel.stream.Stream(streamDetails, streamStore, eventSerializer);
        return stream;
    }

    // A way to pass in a list of event handlers.
    /**
     * Construct a Stream from a stream annotation, a handler, and a list of event handlers
     *
     * @param streamAnnotation The annotation on the handler method
     * @param handler The consumer that will be called when the stream is updated.
     * @param eventHandlers A list of all the event handlers that are registered with the stream.
     * @return A Stream object.
     */
    public com.dewdrop.read.readmodel.stream.Stream constructStream(Stream streamAnnotation, Consumer handler, List<Class<? extends Event>> eventHandlers) {
        StreamDetails streamDetails = fromStreamAnnotation(streamAnnotation, handler, eventHandlers);
        com.dewdrop.read.readmodel.stream.Stream stream = new com.dewdrop.read.readmodel.stream.Stream(streamDetails, streamStore, eventSerializer);
        return stream;
    }

    /**
     * Construct a Stream for an event stream from a handler, and an event class
     *
     * @param handler The handler that will be called when an event is received.
     * @param eventClass The class of the event that you want to listen to.
     * @return A stream.
     */
    public <T extends Event> com.dewdrop.read.readmodel.stream.Stream constructStreamForEvent(Consumer handler, Class<? extends Event> eventClass) {
        StreamDetails streamDetails = fromEvent(handler, eventClass);
        com.dewdrop.read.readmodel.stream.Stream stream = new com.dewdrop.read.readmodel.stream.Stream(streamDetails, streamStore, eventSerializer);
        return stream;
    }
}
