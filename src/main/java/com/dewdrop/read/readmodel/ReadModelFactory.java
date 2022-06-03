package com.dewdrop.read.readmodel;

import com.dewdrop.read.StreamDetails;
import com.dewdrop.read.readmodel.annotation.Stream;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.serialize.EventSerializer;
import com.dewdrop.utils.EventHandlerUtils;
import com.dewdrop.utils.ReadModelUtils;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ReadModelFactory {
    StreamStore streamStore;
    EventSerializer eventSerializer;
    StreamDetailsFactory streamDetailsFactory;

    public ReadModelFactory(StreamStore streamStore, EventSerializer eventSerializer, StreamDetailsFactory streamDetailsFactory) {
        this.streamStore = streamStore;
        this.eventSerializer = eventSerializer;
        this.streamDetailsFactory = streamDetailsFactory;
    }

    public Optional<com.dewdrop.read.readmodel.ReadModel<Message>> constructReadModel(Class<?> target) {
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

        com.dewdrop.read.readmodel.ReadModel<Message> value = contruct(instance).get();
        if (value != null) { return Optional.ofNullable(value); }

        return Optional.empty();
    }

    public <T extends Message> Supplier<com.dewdrop.read.readmodel.ReadModel<T>> contruct(java.lang.Object target) {
        return () -> {
            com.dewdrop.read.readmodel.ReadModel<T> readModel = ReadModelUtils.createReadModel(target);

            Stream[] streams = target.getClass().getAnnotationsByType(Stream.class);
            List<Class<?>> eventHandlers = EventHandlerUtils.getFirstParameterForEventHandlerMethods(readModel.getCachedStateObjectType());
            Arrays.stream(streams).forEach(streamAnnotation -> {
                StreamDetails streamDetails = streamDetailsFactory.fromStreamAnnotation(streamAnnotation, (Consumer) readModel.handler(), eventHandlers);
                com.dewdrop.read.readmodel.stream.Stream stream = new com.dewdrop.read.readmodel.stream.Stream(streamDetails, streamStore, eventSerializer);
                readModel.addStream(stream);
            });
            return readModel;
        };
    }
}
