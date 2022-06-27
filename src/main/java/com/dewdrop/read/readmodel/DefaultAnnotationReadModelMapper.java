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

    /**
     * > Registers the query objects to the read models based on the first parameter of the method For
     * Example: @EventHandler public void query(GetUserByIdQuery query) { ... } Which then would
     * register GetUserByIdQuery.class -> UserReadModel.class This is how dewdrop.executeQuery(query)
     * works
     *
     * @param readModelClass The class of the read model
     * @param instance The instance of the read model that was constructed.
     */
    void registerQueryHandlers(Class<?> readModelClass, Optional<ReadModelConstructed> instance) {
        List<Method> queryHandlerMethods = ReadModelUtils.getQueryHandlerMethods(readModelClass);
        for (Method queryHandlerMethod : queryHandlerMethods) {
            QUERY_TO_READ_MODEL_CLASS.put(queryHandlerMethod.getParameterTypes()[0], readModelClass);

            if (instance.isPresent()) {
                addToQueryReadModelCache(instance.get().getReadModel(), queryHandlerMethod);
            }
        }
    }

    /**
     * This will actually add the query and read model to our cache
     *
     * @param readModel The read model that will be used to handle the query.
     * @param queryHandlerMethod The method that handles the query.
     */
    void addToQueryReadModelCache(final ReadModel<Event> readModel, final Method queryHandlerMethod) {
        requireNonNull(readModel, "ReadModel is required");
        requireNonNull(queryHandlerMethod, "queryHandlerMethod is required");

        Class<?> parameterType = queryHandlerMethod.getParameterTypes()[0];
        log.info("Registering @QueryHandler for {} to be handled by {}", parameterType.getSimpleName(), readModel.getReadModel().getClass().getSimpleName());
        QUERY_TO_READ_MODEL.computeIfAbsent(parameterType, k -> readModel);
    }

    /**
     * > For each method annotated with @OnEvent, create a read model that will be used to invoke the
     * method when the event is received
     */
    void registerOnEvents() {
        Set<Method> annotatedMethods = DewdropAnnotationUtils.getAnnotatedMethods(OnEvent.class);
        annotatedMethods.stream().forEach(annotatedMethod -> {
            readModelFactory.createReadModelForOnEvent(annotatedMethod);
        });
    }

    /**
     * This method looks for the read model based on the query object First it looks in the existing
     * registered ReadModels, then it looks in the ephemeral ReadModels if they don't exist it will
     * create a new one and cache it for the appropriate amount of time Which is determined by
     * the @ReadModel annotation field destroyInMinutesUnused -1 = NEVER_DESTROY 0 = DESTROY_IMMEDIATELY
     * N = destroy after n minutes unused
     *
     * @param query The query object that is passed in from the client.
     * @return Optional<ReadModel<Event>>
     */
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
            return Optional.ofNullable(createAndCacheEphemeralReadModel(readModelClass));
        }
        return Optional.empty();
    }

    /**
     * Based on the readModelClass, construct and cache an ephemeral read model -1 = NEVER_DESTROY 0 =
     * DESTROY_IMMEDIATELY N = destroy after n minutes unused
     *
     * @param readModelClass The class of the read model to be created.
     * @return A read model.
     */
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
