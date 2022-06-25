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

    private StreamDetails fromStreamAnnotation(Stream streamAnnotation, Consumer<Event> eventHandler, List<Class<? extends Event>> messageTypes) {
        requireNonNull(streamAnnotation, "StreamAnnotation is required");
        requireNonNull(eventHandler, "EventHandler is required");

        return StreamDetails.builder().streamType(streamAnnotation.streamType()).direction(streamAnnotation.direction()).eventHandler(eventHandler).streamNameGenerator(streamNameGenerator).messageTypes(messageTypes).name(streamAnnotation.name())
                        .subscribed(streamAnnotation.subscribed()).create();
    }

    private StreamDetails fromAggregateRoot(final AggregateRoot aggregateRoot, final UUID overrideId) {
        UUID id = Optional.ofNullable(overrideId).orElseGet(() -> AggregateIdUtils.getAggregateId(aggregateRoot).orElse(null));
        requireNonNull(id, "aggregateId is required");

        return StreamDetails.builder().streamType(StreamType.AGGREGATE).direction(Direction.FORWARD).aggregateRoot(aggregateRoot).streamNameGenerator(streamNameGenerator).id(id).create();
    }

    private <T extends Event> StreamDetails fromEvent(Consumer<Event> eventConsumer, Class<? extends Event> eventClass) {
        return StreamDetails.builder().streamType(StreamType.EVENT).direction(Direction.FORWARD).eventHandler(eventConsumer).streamNameGenerator(streamNameGenerator).messageTypes(List.of(eventClass)).name(eventClass.getSimpleName()).subscribed(true)
                        .subscriptionStartStrategy(SubscriptionStartStrategy.START_END_ONLY).create();
    }

    public com.dewdrop.read.readmodel.stream.Stream constructStream(AggregateRoot aggregateRoot, UUID aggregateRootId) {
        StreamDetails streamDetails = fromAggregateRoot(aggregateRoot, aggregateRootId);
        com.dewdrop.read.readmodel.stream.Stream stream = new com.dewdrop.read.readmodel.stream.Stream(streamDetails, streamStore, eventSerializer);
        return stream;
    }

    public com.dewdrop.read.readmodel.stream.Stream constructStream(Stream streamAnnotation, Consumer handler, List<Class<? extends Event>> eventHandlers) {
        StreamDetails streamDetails = fromStreamAnnotation(streamAnnotation, handler, eventHandlers);
        com.dewdrop.read.readmodel.stream.Stream stream = new com.dewdrop.read.readmodel.stream.Stream(streamDetails, streamStore, eventSerializer);
        return stream;
    }

    public <T extends Event> com.dewdrop.read.readmodel.stream.Stream constructStreamForEvent(Consumer handler, Class<? extends Event> eventClass) {
        StreamDetails streamDetails = fromEvent(handler, eventClass);
        com.dewdrop.read.readmodel.stream.Stream stream = new com.dewdrop.read.readmodel.stream.Stream(streamDetails, streamStore, eventSerializer);
        return stream;
    }
}
