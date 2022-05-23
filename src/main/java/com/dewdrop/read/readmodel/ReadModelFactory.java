package com.dewdrop.read.readmodel;

import com.dewdrop.read.StreamDetails;
import com.dewdrop.read.readmodel.annotation.ReadModel;
import com.dewdrop.read.readmodel.annotation.Stream;
import com.dewdrop.read.readmodel.cache.CacheManager;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.serialize.EventSerializer;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ReadModelFactory {
    StreamStore streamStore;
    EventSerializer eventSerializer;
    StreamDetailsFactory streamDetailsFactory;
    CacheManager cacheManager;

    public ReadModelFactory(StreamStore streamStore, EventSerializer eventSerializer, StreamDetailsFactory streamDetailsFactory, CacheManager cacheManager) {
        this.streamStore = streamStore;
        this.eventSerializer = eventSerializer;
        this.streamDetailsFactory = streamDetailsFactory;
        this.cacheManager = cacheManager;
    }

    public Optional<CacheableReadModel> constructReadModel(Class<?> target) {
        Object instance;
        try {
            instance = target
                .getConstructor()
                .newInstance();
        } catch (InstantiationException | InvocationTargetException e) {
            log.error("Error instantiating read model", e);
            return Optional.empty();
        } catch (NoSuchMethodException | IllegalAccessException e) {
            log.error("No default constructor found for:{}", target.getClass()
                .getName(), e);
            return Optional.empty();
        }

        CacheableReadModel<Object> value = contruct(instance).get();
        if (value != null) {return Optional.ofNullable(value);}

        return Optional.empty();
    }

    public <T extends Message> Supplier<CacheableReadModel<Object>> contruct(java.lang.Object target) {
        return () -> {
            ReadModel annotation = target.getClass()
                .getAnnotation(ReadModel.class);
            Class<?> resultClass = annotation.resultClass();
            CacheableReadModel<Object> readModel = new CacheableReadModel<>(target, resultClass, cacheManager);
            Stream[] streams = target.getClass().getAnnotationsByType(Stream.class);

            Arrays.stream(streams).forEach(streamAnnotation -> {
                StreamDetails streamDetails = streamDetailsFactory.fromStreamAnnotation(streamAnnotation, readModel.handler());
                com.dewdrop.read.readmodel.stream.Stream stream = new com.dewdrop.read.readmodel.stream.Stream(streamDetails, streamStore, eventSerializer);
                readModel.addStream(stream);
            });
            return readModel;
        };
    }
}
