package com.dewdrop.read.readmodel;

import static java.util.stream.Collectors.joining;

import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.serialize.EventSerializer;
import com.dewdrop.utils.ReadModelUtils;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class DefaultAnnotationReadModelMapper implements ReadModelMapper {
    public DefaultAnnotationReadModelMapper() {}

    protected StreamStore streamStore;
    protected EventSerializer eventSerializer;
    protected StreamDetailsFactory streamDetailsFactory;
    protected ReadModelFactory readModelFactory;
    protected Map<Class<?>, ReadModel<Object>> readModels = new HashMap<>();
    protected Map<Class<?>, ReadModel<Object>> queryToReadModelMethod = new HashMap<>();

    public void init(StreamStore streamStore, EventSerializer eventSerializer, StreamDetailsFactory streamDetailsFactory, ReadModelFactory readModelFactory) {
        this.streamStore = streamStore;
        this.eventSerializer = eventSerializer;
        this.streamDetailsFactory = streamDetailsFactory;
        this.readModelFactory = readModelFactory;

        registerReadModels();
    }

    protected void registerReadModels() {
        List<Class<?>> annotatedReadModels = ReadModelUtils.getAnnotatedReadModels();
        annotatedReadModels.forEach(readModelClass -> {
            Optional<ReadModel> readModel = readModelFactory.constructReadModel(readModelClass);
            if (readModel.isPresent()) {
                ReadModel<Object> value = readModel.get();
                value.subscribe();
                value.getStreams().forEach(stream -> readModels.put(stream.getStreamDetails().getMessageType(), value));

                List<Method> methods = getQueryHandlerMethods(value);
                methods.forEach(method -> {
                    Class<?> parameterType = method.getParameterTypes()[0];
                    String streams = value.getStreams().stream().map(stream -> stream.getStreamDetails().getStreamName()).collect(joining(","));
                    log.info("Registering @QueryHandler for {} to be handled by {}", parameterType.getSimpleName(), streams);
                    queryToReadModelMethod.computeIfAbsent(parameterType, k -> value);
                });
            }
        });
    }

    public List<Method> getQueryHandlerMethods(ReadModel<Object> value) {
        Object instance = value.getReadModel();
        List<Method> methods = ReadModelUtils.getQueryHandlerMethods(instance);
        return methods;
    }

    @Override
    public ReadModel<Object> getReadModelByQuery(Object query) {
        return queryToReadModelMethod.get(query.getClass());
    }
}
