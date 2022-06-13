package com.dewdrop.read.readmodel;

import static java.util.Objects.requireNonNull;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.read.StreamDetails;
import com.dewdrop.read.StreamType;
import com.dewdrop.read.readmodel.annotation.Stream;
import com.dewdrop.structure.StreamNameGenerator;
import com.dewdrop.structure.api.Message;
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

    StreamStore streamStore;
    EventSerializer eventSerializer;


    public StreamFactory(StreamStore streamStore, EventSerializer eventSerializer, StreamNameGenerator streamNameGenerator) {
        requireNonNull(streamStore, "streamStore is required");
        requireNonNull(eventSerializer, "eventSerializer is required");
        requireNonNull(streamNameGenerator, "StreamNameGenerator is required");

        this.streamStore = streamStore;
        this.eventSerializer = eventSerializer;
        this.streamNameGenerator = streamNameGenerator;
    }

    private StreamDetails fromStreamAnnotation(Stream streamAnnotation, Consumer<Message> eventHandler, List<Class<?>> messageTypes) {
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

    public com.dewdrop.read.readmodel.stream.Stream constructStream(AggregateRoot aggregateRoot, UUID aggregateRootId) {
        StreamDetails streamDetails = fromAggregateRoot(aggregateRoot, aggregateRootId);
        com.dewdrop.read.readmodel.stream.Stream stream = new com.dewdrop.read.readmodel.stream.Stream(streamDetails, streamStore, eventSerializer);
        return stream;
    }

    public com.dewdrop.read.readmodel.stream.Stream constructStream(Stream streamAnnotation, Consumer handler, List<Class<?>> eventHandlers) {
        StreamDetails streamDetails = fromStreamAnnotation(streamAnnotation, handler, eventHandlers);
        com.dewdrop.read.readmodel.stream.Stream stream = new com.dewdrop.read.readmodel.stream.Stream(streamDetails, streamStore, eventSerializer);
        return stream;
    }
}
