package org.dewdrop.read.readmodel;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.dewdrop.read.readmodel.annotation.Stream;
import org.dewdrop.read.readmodel.stream.StreamFactory;
import org.dewdrop.structure.api.Event;
import org.dewdrop.structure.datastore.StreamStore;
import org.dewdrop.structure.serialize.EventSerializer;
import org.dewdrop.utils.ReadModelUtils;

/**
 * Factory class is responsible for creating read models
 */
@Log4j2
public class ReadModelFactory {
    StreamStore streamStore;
    EventSerializer eventSerializer;
    StreamFactory streamFactory;

    public ReadModelFactory(StreamStore streamStore, EventSerializer eventSerializer, StreamFactory streamFactory) {
        requireNonNull(streamStore, "streamStore is required");
        requireNonNull(eventSerializer, "eventSerializer is required");
        requireNonNull(streamFactory, "streamFactory is required");

        this.streamStore = streamStore;
        this.eventSerializer = eventSerializer;
        this.streamFactory = streamFactory;
    }

    /**
     * Construct a read model from a read model target
     *
     * @param readModelTarget The class of the read model to be constructed.
     * @return Optional<ReadModelConstructed>
     */
    public Optional<ReadModelConstructed> constructReadModel(Class<?> readModelTarget) {
        Optional<ReadModelWrapper> optReadModelWrapper = ReadModelWrapper.of(readModelTarget);

        if (optReadModelWrapper.isPresent()) {
            ReadModelWrapper instance = optReadModelWrapper.get();
            log.info("Created @ReadModel {} - ephemeral:{}", instance.getClass().getSimpleName(), ReadModelUtils.isEphemeral(readModelTarget));
            ReadModel<Event> value = construct(instance);
            if (value != null) {
                value.subscribe();
                return Optional.of(new ReadModelConstructed(value));
            }
        }

        log.error("Could not create @ReadModel {}", readModelTarget.getSimpleName());
        return Optional.empty();
    }

    /**
     * It takes a read model object and returns a read model object with streams attached to it
     *
     * @param readModelWrapper The object that is annotated with @ReadModel
     * @return A ReadModel object
     */
    <T extends Event> ReadModel<T> construct(ReadModelWrapper readModelWrapper) {
        ReadModel<T> readModel = ReadModelUtils.createReadModel(readModelWrapper);

        List<Stream> streams = readModelWrapper.getStreamAnnotations();
        if (streams.isEmpty()) {
            log.error("No @Stream annotation found on {} - This is used to know what stream to read from and is required", readModelWrapper.getClass().getSimpleName());
            return null;
        }

        streams.forEach(streamAnnotation -> {
            try {
                org.dewdrop.read.readmodel.stream.Stream stream = streamFactory.constructStreamFromStream(streamAnnotation, readModel);
                readModel.addStream(stream);
                log.info("Creating Stream for stream:{} - subscribed:{} for ReadModel:{}", stream.getStreamDetails().getStreamName(), stream.getStreamDetails().isSubscribed(), readModelWrapper.getClass().getSimpleName());
            } catch (IllegalArgumentException e) {
                log.error("Could not create stream for {} for ReadModel:{} - skipping", streamAnnotation.name(), readModel.getReadModelWrapper().getOriginalReadModelClass().getSimpleName());
            }
        });
        return readModel;
    }

    /**
     * This method creates a read model for an @OnEvent annotated method
     *
     * @param annotatedMethod The method that is annotated with @OnEvent
     * @return A ReadModel
     */
    public <T extends Event> ReadModel<T> createReadModelForOnEvent(Method annotatedMethod) {
        Class<?> declaringClass = annotatedMethod.getDeclaringClass();
        Optional<ReadModelWrapper> onEventService = ReadModelWrapper.of(declaringClass);
        if (onEventService.isEmpty()) {
            log.error("Unable to construct read model for @OnEvent method:{} in target class:{}", annotatedMethod.getName(), declaringClass.getSimpleName());
            return null;
        }
        ReadModelWrapper readModelWrapper = onEventService.get();
        ReadModel<T> readModel = new ReadModel<>(readModelWrapper, Optional.empty());
        Class<?>[] params = annotatedMethod.getParameterTypes();
        if (params.length == 0) {
            log.error("Invalid @OnEvent {}.{}() - First parameter must have a parameter that extends org.dewdrop.structure.api.Event like handle(ExampleAccountCreated event)", declaringClass.getSimpleName(), annotatedMethod.getName());
            return null;
        }
        Class<?> eventType = params[0];
        if (!Event.class.isAssignableFrom(eventType)) {
            log.error("Invalid first parameter:{} for @OnEvent {}.{}({} event) - First parameter must extend org.dewdrop.structure.api.Event", eventType.getSimpleName(), declaringClass.getSimpleName(), annotatedMethod.getName(),
                            eventType.getSimpleName());
            return null;
        }
        log.info("Created @OnEvent ReadModel {} for eventType:{}", readModelWrapper.getClass().getSimpleName(), eventType.getSimpleName());
        org.dewdrop.read.readmodel.stream.Stream stream = streamFactory.constructStreamForEvent(readModel.handler(), (Class<? extends Event>) eventType);
        readModel.addStream(stream);
        readModel.subscribe();
        return readModel;
    }


}