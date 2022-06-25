package com.dewdrop.read.readmodel.stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.structure.StreamNameGenerator;
import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.read.Direction;
import com.dewdrop.utils.AggregateIdUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Data;

@Data
public class StreamDetails {
    private StreamType streamType;
    private Direction direction;
    private List<Class<? extends Event>> messageTypes;
    private String streamName;
    private Consumer<Event> eventHandler;
    private StreamNameGenerator streamNameGenerator;
    private boolean subscribed;
    private SubscriptionStartStrategy subscriptionStartStrategy;


    @Builder(buildMethodName = "create")
    public StreamDetails(StreamType streamType, String name, List<Class<? extends Event>> messageTypes, Consumer<Event> eventHandler, Direction direction, AggregateRoot aggregateRoot, UUID id, Boolean subscribed,
                    StreamNameGenerator streamNameGenerator, SubscriptionStartStrategy subscriptionStartStrategy) {
        this.streamType = streamType;
        this.messageTypes = messageTypes;
        this.eventHandler = eventHandler;
        this.direction = direction;
        this.streamNameGenerator = streamNameGenerator;
        this.subscribed = Optional.ofNullable(subscribed).orElse(true);
        this.subscriptionStartStrategy = Optional.ofNullable(subscriptionStartStrategy).orElse(SubscriptionStartStrategy.READ_ALL_START_END);
        switch (streamType) {
            case EVENT:
                this.streamName = streamNameGenerator.generateForEvent(name);
                break;
            case AGGREGATE:
                requireNonNull(aggregateRoot, "AggregateRoot is required to create a StreamDetails of StreamType.AGGREGATE");

                UUID verifiedId = Optional.ofNullable(id).orElseGet(() -> AggregateIdUtils.getAggregateId(aggregateRoot).orElseThrow(() -> new IllegalArgumentException("No ID found for AggregateRoot")));
                this.streamName = streamNameGenerator.generateForAggregate(aggregateRoot.getTarget().getClass(), verifiedId);
                break;
            case CATEGORY:
            default:
                this.streamName = streamNameGenerator.generateForCategory(name);
                break;
        }
    }

    public String getMessageTypeNames() {
        return getMessageTypes().stream().map(Class::getSimpleName).collect(joining(", "));
    }
}
