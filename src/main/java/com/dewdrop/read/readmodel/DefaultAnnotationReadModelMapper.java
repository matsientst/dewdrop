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
    protected Map<Class<?>, CacheableCategoryReadModel<Message, Object>> readModels = new HashMap<>();
    protected Map<Class<?>, CacheableCategoryReadModel<Message, Object>> queryToReadModelMethod = new HashMap<>();

    public void init(StreamStore streamStore, EventSerializer eventSerializer) {
        this.streamStore = streamStore;
        this.eventSerializer = eventSerializer;

        registerReadModels();
    }

    protected void registerReadModels() {
        List<Class<?>> annotatedReadModels = ReadModelUtils.getAnnotatedReadModels();
        annotatedReadModels.forEach(readModelClass -> {
            Optional<CacheableCategoryReadModel> readModel = ReadModelUtils.constructReadModel(readModelClass, streamStore, eventSerializer);
            if (readModel.isPresent()) {
                CacheableCategoryReadModel<Message, Object> value = readModel.get();
                value.readAndSubscribe();
                readModels.put(value.getMessageType(), value);

                List<Method> methods = getQueryHandlerMethods(value);
                methods.forEach(method -> {
                    Class<?> parameterType = method.getParameterTypes()[0];
                    queryToReadModelMethod.computeIfAbsent(parameterType, k -> value);
                });
            }
        });
    }

    public List<Method> getQueryHandlerMethods(CacheableCategoryReadModel<Message, Object> value) {
        Object instance = value.getReadModel();
        List<Method> methods = ReadModelUtils.getQueryMethods(instance);
        return methods;
    }

    @Override
    public CacheableCategoryReadModel<Message, Object> getReadModelByQuery(Object query) {
        return queryToReadModelMethod.get(query.getClass());
    }
}
