package com.dewdrop.read.readmodel;

import static java.util.Objects.requireNonNull;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.read.StreamDetails;
import com.dewdrop.read.StreamType;
import com.dewdrop.read.readmodel.annotation.Stream;
import com.dewdrop.structure.StreamNameGenerator;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.read.Direction;
import com.dewdrop.utils.AggregateIdUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class StreamDetailsFactory {
    private StreamNameGenerator streamNameGenerator;

    public StreamDetailsFactory(StreamNameGenerator streamNameGenerator) {
        this.streamNameGenerator = streamNameGenerator;
    }

    public StreamDetails fromStreamAnnotation(Stream streamAnnotation, Consumer<Message> eventHandler, List<Class<?>> messageTypes) {
        requireNonNull(streamAnnotation, "StreamAnnotation is required");
        requireNonNull(eventHandler, "EventHandler is required");

        return StreamDetails.builder().streamType(streamAnnotation.streamType()).direction(streamAnnotation.direction()).eventHandler(eventHandler).streamNameGenerator(streamNameGenerator).messageTypes(messageTypes).name(streamAnnotation.name())
                        .subscribed(streamAnnotation.subscribed()).create();
    }

    public StreamDetails fromAggregateRoot(final AggregateRoot aggregateRoot, final UUID overrideId) {
        UUID id = Optional.ofNullable(overrideId).orElseGet(() -> AggregateIdUtils.getAggregateId(aggregateRoot).orElse(null));
        requireNonNull(id, "aggregateId is required");

        return StreamDetails.builder().streamType(StreamType.AGGREGATE).direction(Direction.FORWARD).aggregateRoot(aggregateRoot).streamNameGenerator(streamNameGenerator).id(id).create();
    }
}
