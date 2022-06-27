package com.dewdrop.read.readmodel;

import static java.util.Objects.requireNonNull;

import com.dewdrop.read.readmodel.annotation.OnEvent;
import com.dewdrop.read.readmodel.annotation.Stream;
import com.dewdrop.read.readmodel.stream.StreamFactory;
import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.serialize.EventSerializer;
import com.dewdrop.utils.DependencyInjectionUtils;
import com.dewdrop.utils.EventHandlerUtils;
import com.dewdrop.utils.ReadModelUtils;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;

/**
 * > Factory class is responsible for creating read models
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
        Optional<?> optInstance = DependencyInjectionUtils.getInstance(readModelTarget);

        if (optInstance.isPresent()) {
            Object instance = optInstance.get();
            log.info("Created @ReadModel {} - ephemeral:{}", instance.getClass().getSimpleName(), ReadModelUtils.isEphemeral(readModelTarget));
            com.dewdrop.read.readmodel.ReadModel<Event> value = construct(instance);
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
     * @param target The object that is annotated with @ReadModel
     * @return A ReadModel object
     */
    <T extends Event> ReadModel<T> construct(Object target) {
        ReadModel<T> readModel = ReadModelUtils.createReadModel(target);

        Stream[] streams = target.getClass().getAnnotationsByType(Stream.class);
        if (streams.length == 0) {
            log.error("No @Stream annotation found on {} - This is used to know what stream to read from and is required", target.getClass().getSimpleName());
            return null;
        }

        List<Class<? extends Event>> eventHandlers = EventHandlerUtils.getEventHandlers(readModel);
        Arrays.stream(streams).forEach(streamAnnotation -> {
            com.dewdrop.read.readmodel.stream.Stream stream = streamFactory.constructStream(streamAnnotation, readModel.handler(), eventHandlers);
            readModel.addStream(stream);
            log.info("Creating Stream for stream:{} - subscribed:{} for ReadModel:{}", stream.getStreamDetails().getStreamName(), stream.getStreamDetails().isSubscribed(), target.getClass().getSimpleName());
        });
        return readModel;
    }

    /**
     * This method creates a read model for an @OnEvent annotated method
     *
     * @param annotatedMethod The method that is annotated with @OnEvent
     * @return A ReadModel
     */
    public <T extends Event> com.dewdrop.read.readmodel.ReadModel<T> createReadModelForOnEvent(Method annotatedMethod) {
        Optional<Object> onEventService = DependencyInjectionUtils.getInstance(annotatedMethod.getDeclaringClass());
        if (onEventService.isEmpty()) {
            log.error("Unable to construct read model for @OnEvent method:{} in target class:{}", annotatedMethod.getName(), annotatedMethod.getDeclaringClass().getSimpleName());
            return null;
        }
        Object instance = onEventService.get();
        com.dewdrop.read.readmodel.ReadModel<T> readModel = new com.dewdrop.read.readmodel.ReadModel<>(instance, Optional.empty());
        Class<?>[] params = annotatedMethod.getParameterTypes();
        if (params.length == 0) {
            log.error("Invalid @OnEvent {}.{}() - First parameter must have a parameter that extends com.dewdrop.structure.api.Event like handle(ExampleAccountCreated event)", annotatedMethod.getDeclaringClass().getSimpleName(),
                            annotatedMethod.getName());
            return null;
        }
        Class<?> eventType = params[0];
        if (!Event.class.isAssignableFrom(eventType)) {
            log.error("Invalid first parameter:{} for @OnEvent {}.{}({} event) - First parameter must extend com.dewdrop.structure.api.Event", eventType.getSimpleName(), annotatedMethod.getDeclaringClass().getSimpleName(), annotatedMethod.getName(),
                            eventType.getSimpleName());
            return null;
        }
        log.info("Created @OnEvent ReadModel {} for eventType:{}", instance.getClass().getSimpleName(), eventType.getSimpleName());
        com.dewdrop.read.readmodel.stream.Stream stream = streamFactory.constructStreamForEvent(readModel.handler(), (Class<? extends Event>) eventType);
        readModel.addStream(stream);
        readModel.subscribe();
        return readModel;
    }


}
