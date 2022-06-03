package com.dewdrop.read.readmodel;

import com.dewdrop.structure.api.Message;
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
    // protected Map<Class<?>, ReadModel<Object>> readModels = new HashMap<>();
    protected Map<Class<?>, ReadModel<Message>> queryToReadModelMethod = new HashMap<>();

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
            Optional<ReadModel<Message>> readModel = readModelFactory.constructReadModel(readModelClass);
            if (readModel.isPresent()) {
                ReadModel<Message> value = readModel.get();
                value.subscribe();

                List<Method> methods = getQueryHandlerMethods(value);
                methods.forEach(method -> {
                    Class<?> parameterType = method.getParameterTypes()[0];
                    log.info("Registering @QueryHandler for {} to be handled by {}", parameterType.getSimpleName(), value.getReadModel().getClass().getSimpleName());
                    queryToReadModelMethod.computeIfAbsent(parameterType, k -> value);
                });
            }
        });
    }

    public List<Method> getQueryHandlerMethods(ReadModel<Message> value) {
        Object instance = value.getReadModel();
        List<Method> methods = ReadModelUtils.getQueryHandlerMethods(instance);
        return methods;
    }

    @Override
    public ReadModel<Message> getReadModelByQuery(Object query) {
        return queryToReadModelMethod.get(query.getClass());
    }
}
