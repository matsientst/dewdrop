package com.dewdrop.read;

import static java.util.stream.Collectors.joining;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.structure.StreamNameGenerator;
import com.dewdrop.structure.api.Message;
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
    private String name;
    private Direction direction;
    private List<Class<?>> messageTypes;
    private String streamName;
    private Consumer<Message> eventHandler;
    private StreamNameGenerator streamNameGenerator;
    private boolean subscribed;

    @Builder(buildMethodName = "create")
    public StreamDetails(StreamType streamType, String name, List<Class<?>> messageTypes, Consumer<Message> eventHandler, Direction direction, AggregateRoot aggregateRoot, UUID id, Boolean subscribed, StreamNameGenerator streamNameGenerator) {
        this.streamType = streamType;
        this.name = name;
        this.messageTypes = messageTypes;
        this.eventHandler = eventHandler;
        this.direction = direction;
        this.streamNameGenerator = streamNameGenerator;
        this.subscribed = Optional.ofNullable(subscribed).orElse(true);
        switch (streamType) {
            case EVENT:
                this.streamName = streamNameGenerator.generateForEvent(name);
                break;
            case AGGREGATE:
                UUID verifiedId = Optional.ofNullable(id).orElseGet(() -> AggregateIdUtils.getAggregateId(aggregateRoot).orElse(null));
                this.streamName = streamNameGenerator.generateForAggregate(aggregateRoot.getTarget().getClass(), verifiedId);
                break;
            case CATEGORY:
            default:
                this.streamName = streamNameGenerator.generateForCategory(name);
                break;
        }
    }

    public String getMessageTypeNames() {
        return getMessageTypes().stream().map(Class::getSimpleName).collect(joining(","));
    }
}
