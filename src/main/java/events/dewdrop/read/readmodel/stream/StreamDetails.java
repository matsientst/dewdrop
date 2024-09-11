package events.dewdrop.read.readmodel.stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import events.dewdrop.aggregate.AggregateRoot;
import events.dewdrop.structure.StreamNameGenerator;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import events.dewdrop.structure.api.Event;
import events.dewdrop.structure.read.Direction;
import events.dewdrop.utils.AggregateIdUtils;

@Data
public class StreamDetails<T extends Event> {
    private StreamType streamType;
    private Direction direction;
    private List<Class<? extends Event>> messageTypes = new ArrayList<>();
    private String streamName;
    private Consumer<T> eventHandler;
    private StreamNameGenerator streamNameGenerator;
    private boolean subscribed;
    private SubscriptionStartStrategy subscriptionStartStrategy;
    private Optional<Method> startPositionMethod;


    @Builder(buildMethodName = "create")
    public StreamDetails(StreamType streamType, String name, List<Class<? extends Event>> messageTypes, Consumer<T> eventHandler, Direction direction, AggregateRoot aggregateRoot, UUID id, Boolean subscribed, StreamNameGenerator streamNameGenerator,
                    SubscriptionStartStrategy subscriptionStartStrategy, Optional<Method> startPositionMethod) {
        this.streamType = streamType;
        if (CollectionUtils.isNotEmpty(messageTypes)) {
            this.messageTypes.addAll(messageTypes);
        }
        this.eventHandler = eventHandler;
        this.direction = direction;
        this.streamNameGenerator = streamNameGenerator;
        this.subscribed = Optional.ofNullable(subscribed).orElse(true);
        this.startPositionMethod = Optional.ofNullable(startPositionMethod).orElse(Optional.empty());
        this.subscriptionStartStrategy = Optional.ofNullable(subscriptionStartStrategy).orElseGet(() -> getStartPositionMethod().isPresent() ? SubscriptionStartStrategy.START_FROM_POSITION : SubscriptionStartStrategy.READ_ALL_START_END);
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
