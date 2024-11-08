package events.dewdrop.read.readmodel.stream;

import events.dewdrop.aggregate.AggregateRoot;
import events.dewdrop.read.readmodel.ReadModel;
import events.dewdrop.structure.StreamNameGenerator;
import events.dewdrop.structure.api.Event;
import events.dewdrop.structure.datastore.StreamStore;
import events.dewdrop.structure.read.Direction;
import events.dewdrop.structure.serialize.EventSerializer;
import events.dewdrop.utils.AggregateIdUtils;
import events.dewdrop.utils.EventHandlerUtils;
import events.dewdrop.utils.StreamUtils;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Factory for creating {@link events.dewdrop.read.readmodel.stream.Stream}s. You can create a
 * stream for a read model based on the type of the stream.
 *
 * <pre>
 * `constructStreamFromAggregateRoot()` will create a stream for the aggregate root.
 * `constructStreamFromStream()` will create a stream from an @Stream annotation.
 * `constructStreamForEvent()` will create a stream for a specific event.
 * </pre>
 */
@Log4j2
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
     * @param <T> The type event supported by the ReadModel
     * @param streamAnnotation The annotation that was placed on the method
     * @param readModel The read model that the stream is being created for
     * @return StreamDetails
     */
    <T extends Event> StreamDetails fromStreamAnnotation(StreamAnnotationDetails streamAnnotation, ReadModel<T> readModel) {
        List<Class<? extends Event>> eventHandlers = EventHandlerUtils.getEventHandlers(readModel);
        Consumer eventHandler = readModel.handler();
        requireNonNull(streamAnnotation, "StreamAnnotation is required");
        requireNonNull(eventHandler, "EventHandler is required");
        String streamName = streamAnnotation.getStreamName();
        StreamType streamType = streamAnnotation.getStreamType();
        boolean subscribed = streamAnnotation.isSubscribed();
        Direction direction = streamAnnotation.getDirection();

        Optional<Method> streamStartPositionMethod = Optional.empty();
        if (readModel.getInMemoryCacheProcessor().isEmpty()) {
            streamStartPositionMethod = StreamUtils.getStreamStartPositionMethod(streamName, streamType, readModel);
            if (streamStartPositionMethod.isEmpty()) {
                String simpleName = readModel.getReadModelWrapper().getOriginalReadModelClass().getSimpleName();
                log.error("Unable to create a valid stream for the ReadModel: {} - @Stream(name={}, streamType={}) - Create a method decorated with @StreamStartPosition(name = {}, streamType = {}) with the same name and streamType for the stream, which is required if the inMemoryCacheProcessor is not set. This should return a long which is your last position for that stream.",
                                simpleName, streamName, streamType, streamName, streamType);
                throw new IllegalStateException(String.format(
                                "Unable to create a valid stream for the ReadModel: %s - @Stream(name=%s, streamType=%s) - Create a method decorated with @StreamStartPosition(name = %s, streamType = %s) with the same name and streamType for the stream, which is required if the inMemoryCacheProcessor is not set.  This should return a long which is your last position for that stream.",
                                simpleName, streamName, streamType, streamName, streamType));
            }
        }
        return StreamDetails.builder().streamType(streamType).direction(direction).eventHandler(eventHandler).streamNameGenerator(streamNameGenerator).messageTypes(eventHandlers).name(streamName).aggregateName(streamName).subscribed(subscribed)
                        .startPositionMethod(streamStartPositionMethod).create();

    }

    /**
     * It creates a `StreamDetails` object from an `AggregateRoot` object, and a UUID
     *
     * @param aggregateRoot The aggregate root to be serialized.
     * @param overrideId This is the id of the aggregate root. If you don't provide it, the id will be
     *        extracted from the aggregate root.
     * @return A StreamDetails object
     */
    StreamDetails fromAggregateRoot(final AggregateRoot aggregateRoot, final UUID overrideId) {
        return StreamDetails.builder().streamType(StreamType.AGGREGATE).direction(Direction.FORWARD).aggregateName(aggregateRoot.getTarget().getClass().getSimpleName()).id(AggregateIdUtils.getAggregateId(aggregateRoot).orElse(overrideId))
                        .streamNameGenerator(streamNameGenerator).id(overrideId).create();
    }

    /**
     * It creates a StreamDetails object that is used to create a stream that subscribes to an event
     * stream
     *
     * @param readModel The readModel.
     * @param eventClass The class of the event you want to listen to.
     * @return A StreamDetails object.
     */
    <T extends Event> StreamDetails fromEvent(ReadModel<T> readModel, Class<? extends Event> eventClass) {
        String streamName = eventClass.getSimpleName();
        Optional<Method> streamStartPositionMethod = StreamUtils.getStreamStartPositionMethod(streamName, StreamType.EVENT, readModel);
        SubscriptionStartStrategy subscriptionStartStrategy = SubscriptionStartStrategy.START_END_ONLY;
        if (!streamStartPositionMethod.isEmpty()) {
            subscriptionStartStrategy = SubscriptionStartStrategy.START_FROM_POSITION;
        }

        return StreamDetails.builder().streamType(StreamType.EVENT).direction(Direction.FORWARD).eventHandler((Consumer<Event>) readModel.handler()).streamNameGenerator(streamNameGenerator).messageTypes(List.of(eventClass)).name(streamName)
                        .subscribed(true).subscriptionStartStrategy(subscriptionStartStrategy).startPositionMethod(streamStartPositionMethod).create();
    }

    /**
     * Construct a Stream from an AggregateRoot object and a UUID
     *
     * @param aggregateRoot The aggregate root that you want to construct a stream for.
     * @param aggregateRootId The id of the aggregate root.
     * @return A Stream object.
     */
    public events.dewdrop.read.readmodel.stream.Stream constructStreamFromAggregateRoot(AggregateRoot aggregateRoot, UUID aggregateRootId) {
        StreamDetails streamDetails = fromAggregateRoot(aggregateRoot, aggregateRootId);
        events.dewdrop.read.readmodel.stream.Stream stream = new events.dewdrop.read.readmodel.stream.Stream(streamDetails, streamStore, eventSerializer);
        return stream;
    }

    // A way to pass in a list of event handlers.

    /**
     * Construct a stream from a stream annotation and a ReadModel
     *
     * @param <T> The type event supported by the ReadModel
     * @param streamAnnotation The annotation on the ReadModel class
     * @param readModel The ReadModel class that is being constructed.
     * @return A stream.
     */
    public <T extends Event> events.dewdrop.read.readmodel.stream.Stream constructStreamFromStream(StreamAnnotationDetails streamAnnotation, ReadModel<T> readModel) {
        StreamDetails streamDetails = fromStreamAnnotation(streamAnnotation, readModel);
        events.dewdrop.read.readmodel.stream.Stream stream = new events.dewdrop.read.readmodel.stream.Stream(streamDetails, streamStore, eventSerializer);
        return stream;
    }

    /**
     * Construct a Stream for an event stream from a handler, and an event class
     *
     * @param readModel The readModel.
     * @param eventClass The class of the event that you want to listen to.
     * @return A stream.
     */
    public <T extends Event> events.dewdrop.read.readmodel.stream.Stream constructStreamForEvent(ReadModel<T> readModel, Class<? extends Event> eventClass) {
        StreamDetails streamDetails = fromEvent(readModel, eventClass);
        events.dewdrop.read.readmodel.stream.Stream stream = new events.dewdrop.read.readmodel.stream.Stream(streamDetails, streamStore, eventSerializer);
        return stream;
    }
}
