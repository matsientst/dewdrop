package com.dewdrop.read.readmodel;

import static java.util.Objects.requireNonNull;

import com.dewdrop.read.readmodel.annotation.OnEvent;
import com.dewdrop.structure.api.Event;
import com.dewdrop.utils.DewdropAnnotationUtils;
import com.dewdrop.utils.ReadModelUtils;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import net.jodah.expiringmap.ExpiringMap;

/**
 * Discover and register all ReadModel classes
 */
@Log4j2
@Data
public class DefaultAnnotationReadModelMapper implements ReadModelMapper {
    public DefaultAnnotationReadModelMapper() {}

    protected ReadModelFactory readModelFactory;
    protected static Map<Class<?>, Class<?>> QUERY_TO_READ_MODEL_CLASS = new ConcurrentHashMap<>();
    protected static Map<Class<?>, ReadModel<Event>> QUERY_TO_READ_MODEL = new ConcurrentHashMap<>();
    protected static ExpiringMap<Class<?>, ReadModel<Event>> EPHEMERAL_READ_MODELS;

    /**
     * "This function is called when the application starts up, and it registers all the read models
     * that the application will use."
     *
     * @param readModelFactory The factory that will be used to create the read models.
     */
    public void init(ReadModelFactory readModelFactory) {
        requireNonNull(readModelFactory, "ReadModelFactory is required");

        this.readModelFactory = readModelFactory;
        registerReadModels();
    }

    /**
     * > First register all the read models that are annotated with @ReadModel and registers the query
     * handlers for them Then create ReadModels associated with @OnEvent
     */
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

            registerQueryHandlers(readModelClass, instance);
        });
        if (ephemeralCount.get() > 0) {
            EPHEMERAL_READ_MODELS = ExpiringMap.builder().maxSize(ephemeralCount.get()).variableExpiration().build();
        }
        registerOnEvents();
    }

    void registerQueryHandlers(Class<?> readModelClass, Optional<ReadModelConstructed> instance) {
        List<Method> queryHandlerMethods = ReadModelUtils.getQueryHandlerMethods(readModelClass);
        for (Method queryHandlerMethod : queryHandlerMethods) {
            QUERY_TO_READ_MODEL_CLASS.put(queryHandlerMethod.getParameterTypes()[0], readModelClass);

            if (instance.isPresent()) {
                addToQueryReadModelCache(instance.get().getReadModel(), queryHandlerMethod);
            }
        }
    }

    void addToQueryReadModelCache(final ReadModel<Event> readModel, final Method queryHandlerMethod) {
        requireNonNull(readModel, "ReadModel is required");
        requireNonNull(queryHandlerMethod, "queryHandlerMethod is required");

        Class<?> parameterType = queryHandlerMethod.getParameterTypes()[0];
        log.info("Registering @QueryHandler for {} to be handled by {}", parameterType.getSimpleName(), readModel.getReadModel().getClass().getSimpleName());
        QUERY_TO_READ_MODEL.computeIfAbsent(parameterType, k -> readModel);
    }

    void registerOnEvents() {
        Set<Method> annotatedMethods = DewdropAnnotationUtils.getAnnotatedMethods(OnEvent.class);
        annotatedMethods.stream().forEach(annotatedMethod -> {
            readModelFactory.createReadModelForOnEvent(annotatedMethod);
        });
    }

    @Override
    public Optional<ReadModel<Event>> getReadModelByQuery(Object query) {
        Class<?> queryclass = query.getClass();
        if (QUERY_TO_READ_MODEL.containsKey(queryclass)) {
            ReadModel<Event> readModel = QUERY_TO_READ_MODEL.get(queryclass);
            return Optional.of(readModel);
        }
        if (QUERY_TO_READ_MODEL_CLASS.containsKey(queryclass)) {
            Class<?> readModelClass = QUERY_TO_READ_MODEL_CLASS.get(queryclass);
            boolean containsKey = EPHEMERAL_READ_MODELS.containsKey(readModelClass);
            if (containsKey) { return Optional.of(EPHEMERAL_READ_MODELS.get(readModelClass)); }
            log.info("CALLING THE MOTHERFUCKER");
            return Optional.ofNullable(createAndCacheEphemeralReadModel(readModelClass));
        }
        return Optional.empty();
    }

    ReadModel<Event> createAndCacheEphemeralReadModel(Class<?> readModelClass) {
        Optional<ReadModelConstructed> instance = readModelFactory.constructReadModel(readModelClass);
        if (instance.isPresent()) {
            ReadModelConstructed readModelConstructed = instance.get();
            ReadModel<Event> readModel = readModelConstructed.getReadModel();
            int minutes = readModelConstructed.getDestroyInMinutesUnused();
            if (minutes == -1) {
                EPHEMERAL_READ_MODELS.put(readModelClass, readModel, Integer.MAX_VALUE, TimeUnit.DAYS);
            } else if (minutes > 0) {
                EPHEMERAL_READ_MODELS.put(readModelClass, readModel, minutes, TimeUnit.MINUTES);
            }
            return readModel;
        }
        return null;
    }
}
