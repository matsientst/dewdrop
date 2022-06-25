package com.dewdrop.read.readmodel;

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
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ReadModelFactory {
    StreamStore streamStore;
    EventSerializer eventSerializer;
    StreamFactory streamFactory;

    public ReadModelFactory(StreamStore streamStore, EventSerializer eventSerializer, StreamFactory streamFactory) {
        this.streamStore = streamStore;
        this.eventSerializer = eventSerializer;
        this.streamFactory = streamFactory;
    }

    public Optional<ReadModelConstructed> constructReadModel(Class<?> target) {
        Optional<?> optInstance = DependencyInjectionUtils.getInstance(target);

        if (optInstance.isPresent()) {
            Object instance = optInstance.get();
            log.info("Created @ReadModel {} - ephemeral:{}", instance.getClass().getSimpleName(), ReadModelUtils.isEphemeral(target));
            com.dewdrop.read.readmodel.ReadModel<Event> value = contruct(instance).get();
            if (value != null) {
                value.subscribe();
                return Optional.of(new ReadModelConstructed(value));
            }
        }
        return Optional.empty();
    }

    <T extends Event> Supplier<ReadModel<T>> contruct(Object target) {
        return () -> {
            ReadModel<T> readModel = ReadModelUtils.createReadModel(target);

            Stream[] streams = target.getClass().getAnnotationsByType(Stream.class);
            List<Class<? extends Event>> eventHandlers = EventHandlerUtils.getEventHandlers(readModel);
            Arrays.stream(streams).forEach(streamAnnotation -> {
                com.dewdrop.read.readmodel.stream.Stream stream = streamFactory.constructStream(streamAnnotation, readModel.handler(), eventHandlers);
                readModel.addStream(stream);
                log.info("Creating Stream for stream:{} - subscribed:{} for ReadModel:{}", stream.getStreamDetails().getStreamName(), stream.getStreamDetails().isSubscribed(), target.getClass().getSimpleName());
            });
            return readModel;
        };
    }

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
        }
        Class<?> eventType = params[0];
        if (!Event.class.isAssignableFrom(eventType)) {
            log.error("Invalid first parameter:{} for @OnEvent {}.{}({} event) - First parameter must extend com.dewdrop.structure.api.Event", eventType.getSimpleName(), annotatedMethod.getDeclaringClass().getSimpleName(), annotatedMethod.getName(),
                            eventType.getSimpleName());
        }
        log.info("Created @OnEvent ReadModel {} for eventType:{}", instance.getClass().getSimpleName(), eventType.getSimpleName());
        com.dewdrop.read.readmodel.stream.Stream stream = streamFactory.constructStreamForEvent(readModel.handler(), (Class<? extends Event>) eventType);
        readModel.addStream(stream);
        readModel.subscribe();
        return readModel;
    }
}
