package events.dewdrop.utils;

import static java.util.Objects.requireNonNull;

import events.dewdrop.read.readmodel.ReadModelWrapper;
import events.dewdrop.read.readmodel.annotation.DewdropCache;
import events.dewdrop.read.readmodel.annotation.ReadModel;
import events.dewdrop.read.readmodel.cache.InMemoryCacheProcessor;
import events.dewdrop.read.readmodel.cache.MapBackedInMemoryCacheProcessor;
import events.dewdrop.read.readmodel.cache.SingleItemInMemoryCache;
import events.dewdrop.read.readmodel.query.QueryHandler;
import events.dewdrop.structure.api.Message;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

@Log4j2
public class ReadModelUtils {
    ReadModelUtils() {}

    private static ReadModelUtils instance;
    private final Queue<Class<?>> READ_MODEL_CACHE = new ConcurrentLinkedQueue<>();

    public static Queue<Class<?>> getAnnotatedReadModels() {
        ReadModelUtils local = getInstance();
        Queue<Class<?>> READ_MODEL_CACHE = local.READ_MODEL_CACHE;
        if (!READ_MODEL_CACHE.isEmpty()) { return READ_MODEL_CACHE; }

        Set<Class<?>> readModelClasses = DewdropAnnotationUtils.getAnnotatedClasses(ReadModel.class);

        READ_MODEL_CACHE.addAll(readModelClasses);

        if (CollectionUtils.isEmpty(READ_MODEL_CACHE)) {
            log.info("No classes annotated with @ReadModel - Without a ReadModel you cannot query");
        }
        return READ_MODEL_CACHE;
    }

    public static <T extends Message> events.dewdrop.read.readmodel.ReadModel createReadModel(ReadModelWrapper readModelWrapper) {
        Optional<InMemoryCacheProcessor> inMemoryCacheProcessor = createInMemoryCache(readModelWrapper.getOriginalReadModelClass());

        return new events.dewdrop.read.readmodel.ReadModel(readModelWrapper, inMemoryCacheProcessor);
    }

    static Optional<InMemoryCacheProcessor> createInMemoryCache(Class<?> targetClass) {
        Field field = getReadModelCacheField(targetClass);
        if (field == null) {
            log.info("No @DewdropCache field found for {} - User will handle", targetClass.getName());
            return Optional.empty();
        }
        InMemoryCacheProcessor inMemoryCacheProcessor;
        if (Map.class.equals(field.getType())) {
            ParameterizedType type = (ParameterizedType) field.getGenericType();
            Class<?> clazz = (Class<?>) type.getActualTypeArguments()[1];
            inMemoryCacheProcessor = new MapBackedInMemoryCacheProcessor<>(clazz);
        } else {
            inMemoryCacheProcessor = new SingleItemInMemoryCache(field.getType());
        }
        return Optional.of(inMemoryCacheProcessor);
    }

    static Field getReadModelCacheField(Class<?> targetClass) {
        Set<Field> fields = DewdropAnnotationUtils.getAnnotatedFields(targetClass, DewdropCache.class);
        if (fields.isEmpty()) {
            log.info("There is no field marked with @DewdropCache on the ReadModel: {} - This allows the framework to inject the ReadModel state and is required", targetClass.getName());
            return null;
        }
        if (fields.size() > 1) {
            log.info("There are more than 1 fields marked with @DewdropCache on the ReadModel: {}} - The frameworks needs 1 cache item to inject the ReadModel state", targetClass.getName());
            return null;
        }
        Field field = fields.stream().findAny().orElse(null);
        return field;
    }

    public static void processOnEvent(Object target, Message message) {
        EventHandlerUtils.callEventHandler(target, message);
    }


    public static List<Method> getQueryHandlerMethods(Class<?> readModelClass) {
        requireNonNull(readModelClass, "RadModel class is required");

        return MethodUtils.getMethodsListWithAnnotation(readModelClass, QueryHandler.class, true, true);
    }

    static void clear() {
        getInstance().READ_MODEL_CACHE.clear();
    }

    public static ReadModelUtils getInstance() {
        if (instance == null) {
            instance = new ReadModelUtils();
        }
        return instance;
    }

    public static <T> void updateReadModelCacheField(Field field, Object readModel, T item) {
        try {
            FieldUtils.writeField(field, readModel, item, true);
        } catch (IllegalAccessException e) {
            log.warn("Unable to write to the field annotated with the @DewdropCache on the ReadModel: {}", readModel.getClass().getName(), e);
        }
    }

    public static boolean isEphemeral(Class<?> readModelClass) {
        ReadModel annotation = readModelClass.getAnnotation(ReadModel.class);
        if (annotation == null) { return false; }

        return annotation.ephemeral();
    }


    public static Optional<Field> getMatchingReadModelCacheField(ReadModelWrapper readModelWrapper) {
        Field field = getReadModelCacheField(readModelWrapper.getOriginalReadModelClass());
        if (field == null) { return Optional.empty(); }

        Field result = FieldUtils.getField(readModelWrapper.getReadModel().getClass(), field.getName(), true);
        return Optional.of(result);
    }
}
