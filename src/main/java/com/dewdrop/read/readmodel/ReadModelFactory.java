package com.dewdrop.read.readmodel;

import com.dewdrop.read.readmodel.annotation.Stream;
import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.serialize.EventSerializer;
import com.dewdrop.utils.EventHandlerUtils;
import com.dewdrop.utils.ReadModelUtils;
import java.lang.reflect.InvocationTargetException;
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
        Object instance;
        try {
            instance = target.getConstructor().newInstance();
        } catch (InstantiationException | InvocationTargetException e) {
            log.error("Error instantiating read model", e);
            return Optional.empty();
        } catch (NoSuchMethodException | IllegalAccessException e) {
            log.error("No default constructor found for:{}", target.getClass().getName(), e);
            return Optional.empty();
        }
        log.info("Created @ReadModel {} - ephemeral:{}", instance.getClass().getSimpleName(), ReadModelUtils.isEphemeral(target));

        com.dewdrop.read.readmodel.ReadModel<Event> value = contruct(instance).get();
        if (value != null) {
            value.subscribe();
            return Optional.of(new ReadModelConstructed(value));
        }

        return Optional.empty();
    }

    <T extends Event> Supplier<com.dewdrop.read.readmodel.ReadModel<T>> contruct(java.lang.Object target) {
        return () -> {
            com.dewdrop.read.readmodel.ReadModel<T> readModel = ReadModelUtils.createReadModel(target);

            Stream[] streams = target.getClass().getAnnotationsByType(Stream.class);
            List<Class<?>> eventHandlers = EventHandlerUtils.getEventHandlers(readModel);
            Arrays.stream(streams).forEach(streamAnnotation -> {
                com.dewdrop.read.readmodel.stream.Stream stream = streamFactory.constructStream(streamAnnotation, readModel.handler(), eventHandlers);
                readModel.addStream(stream);
                log.info("Creating Stream for stream:{} - subscribed:{} for ReadModel:{}", stream.getStreamDetails().getStreamName(), stream.getStreamDetails().isSubscribed(), target.getClass().getSimpleName());
            });
            return readModel;
        };
    }
}
