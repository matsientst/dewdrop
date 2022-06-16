package com.dewdrop.read.readmodel;

import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.serialize.EventSerializer;
import com.dewdrop.utils.ReadModelUtils;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import net.jodah.expiringmap.ExpiringMap;

@Log4j2
@Data
public class DefaultAnnotationReadModelMapper implements ReadModelMapper {
    public DefaultAnnotationReadModelMapper() {}

    protected StreamStore streamStore;
    protected EventSerializer eventSerializer;
    protected StreamFactory streamFactory;
    protected ReadModelFactory readModelFactory;
    protected Map<Class<?>, Class<?>> queryToReadModelClass = new ConcurrentHashMap<>();
    protected Map<Class<?>, ReadModel<Event>> queryToReadModel = new ConcurrentHashMap<>();
    ExpiringMap<Class<?>, ReadModel<Event>> ephemeralReadModel;

    public void init(StreamStore streamStore, EventSerializer eventSerializer, StreamFactory streamFactory, ReadModelFactory readModelFactory) {
        this.streamStore = streamStore;
        this.eventSerializer = eventSerializer;
        this.streamFactory = streamFactory;
        this.readModelFactory = readModelFactory;

        registerReadModels();
    }

    protected void registerReadModels() {
        List<Class<?>> annotatedReadModels = ReadModelUtils.getAnnotatedReadModels();
        AtomicInteger ephemeralCount = new AtomicInteger(0);
        annotatedReadModels.forEach(readModelClass -> {
            Optional<ReadModelConstructed> instance = Optional.empty();
            if (!ReadModelUtils.isEphemeral(readModelClass)) {
                instance = readModelFactory.constructReadModel(readModelClass);
            } else {
                ephemeralCount.incrementAndGet();
            }

            List<Method> queryHandlerMethods = ReadModelUtils.getQueryHandlerMethods(readModelClass);
            for (Method queryHandlerMethod : queryHandlerMethods) {
                queryToReadModelClass.put(queryHandlerMethod.getParameterTypes()[0], readModelClass);

                if (instance.isPresent()) {
                    addToQueryReadModelCache(instance.get().getReadModel(), queryHandlerMethod);
                }
            }
        });
        if (ephemeralCount.get() > 0) {
            ephemeralReadModel = ExpiringMap.builder().maxSize(ephemeralCount.get()).variableExpiration().build();
        }
    }

    private void addToQueryReadModelCache(final ReadModel<Event> instance, final Method queryHandlerMethod) {
        Class<?> parameterType = queryHandlerMethod.getParameterTypes()[0];
        log.info("Registering @QueryHandler for {} to be handled by {}", parameterType.getSimpleName(), instance.getReadModel().getClass().getSimpleName());
        queryToReadModel.computeIfAbsent(parameterType, k -> instance);
    }

    @Override
    public ReadModel<Event> getReadModelByQuery(Object query) {
        if (queryToReadModel.containsKey(query.getClass())) { return queryToReadModel.get(query.getClass()); }
        if (queryToReadModelClass.containsKey(query.getClass())) {
            Class<?> readModelClass = queryToReadModelClass.get(query.getClass());
            if (ephemeralReadModel.containsKey(readModelClass)) { return ephemeralReadModel.get(readModelClass); }
            ReadModel<Event> instance = createAndCacheEphemeralReadModel(readModelClass);
            if (instance != null) { return instance; }
        }
        return null;
    }

    private ReadModel<Event> createAndCacheEphemeralReadModel(Class<?> readModelClass) {
        Optional<ReadModelConstructed> instance = readModelFactory.constructReadModel(readModelClass);
        if (instance.isPresent()) {
            ReadModelConstructed readModelConstructed = instance.get();
            ReadModel<Event> readModel = readModelConstructed.getReadModel();
            int minutes = readModelConstructed.getDestroyInMinutesUnused();
            if (minutes == -1) {
                ephemeralReadModel.put(readModelClass, readModel, Integer.MAX_VALUE, TimeUnit.DAYS);
            } else if (minutes > 0) {
                ephemeralReadModel.put(readModelClass, readModel, minutes, TimeUnit.MINUTES);
            }
            return readModel;
        }
        return null;
    }
}
