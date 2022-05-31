package com.dewdrop.utils;

import static java.util.Objects.requireNonNull;

import com.dewdrop.read.readmodel.annotation.DewdropCache;
import com.dewdrop.read.readmodel.annotation.ReadModel;
import com.dewdrop.read.readmodel.cache.CacheManager;
import com.dewdrop.read.readmodel.cache.InMemoryCacheProcessor;
import com.dewdrop.read.readmodel.cache.MapBackedInMemoryCacheProcessor;
import com.dewdrop.read.readmodel.cache.SingleItemInMemoryCache;
import com.dewdrop.read.readmodel.query.QueryHandler;
import com.dewdrop.structure.api.Message;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

@Log4j2
public class ReadModelUtils {
    ReadModelUtils() {}

    private static ReadModelUtils instance;
    private final List<Class<?>> READ_MODEL_CACHE = new ArrayList<>();

    public static List<Class<?>> getAnnotatedReadModels() {
        ReadModelUtils local = getInstance();
        List<Class<?>> READ_MODEL_CACHE = local.READ_MODEL_CACHE;
        if (!READ_MODEL_CACHE.isEmpty()) {return READ_MODEL_CACHE;}

        Set<Class<?>> readModelClasses = DewdropAnnotationUtils.getAnnotatedClasses(ReadModel.class);

        READ_MODEL_CACHE.addAll(readModelClasses);

        if (CollectionUtils.isEmpty(READ_MODEL_CACHE)) {
            log.error("No classes annotated with @ReadModel - Without a ReadModel you cannot query");
        }
        return READ_MODEL_CACHE;
    }

    public static <T extends Message> com.dewdrop.read.readmodel.ReadModel createReadModel(Object target, CacheManager cacheManager) {
        ReadModel annotation = target.getClass()
            .getAnnotation(ReadModel.class);
        Class<?> resultClass = annotation.resultClass();
        InMemoryCacheProcessor inMemoryCacheProcessor = createInMemoryCache(target, cacheManager, resultClass);

        return new com.dewdrop.read.readmodel.ReadModel(target, resultClass, inMemoryCacheProcessor);
    }

    private static InMemoryCacheProcessor createInMemoryCache(Object target, CacheManager cacheManager, Class<?> resultClass) {
        Field field = getReadModelCacheField(target);
        InMemoryCacheProcessor inMemoryCacheProcessor;
        if (Map.class.equals(field.getType())) {
            inMemoryCacheProcessor = new MapBackedInMemoryCacheProcessor<>(resultClass, cacheManager);
        } else {
            inMemoryCacheProcessor = new SingleItemInMemoryCache(resultClass, cacheManager);
        }
        return inMemoryCacheProcessor;
    }

    private static Field getReadModelCacheField(Object target) {
        Set<Field> fields = DewdropAnnotationUtils.getAnnotatedFields(target, DewdropCache.class);
        if (fields.isEmpty()) {
            throw new IllegalArgumentException(String.format("There is no field marked with @DewdropCache on the ReadModel: %s - This allows the framework to inject the ReadModel state and is required", target.getClass()
                .getName()));
        }
        if (fields.size() > 1) {
            throw new IllegalArgumentException(String.format("We have more than 1 field marked with @DewdropCache on the ReadModel: %s - The frameworks needs 1 cache item to inject the ReadModel state", target.getClass()
                .getName()));
        }
        Field field = fields.stream()
            .findAny()
            .orElse(null);
        return field;
    }

    public static void processOnEvent(Object target, Message message) {
        EventHandlerUtils.callEventHandler(target, message);
    }


    public static List<Method> getQueryHandlerMethods(Object instance) {
        requireNonNull(instance, "Object is required");

        return MethodUtils.getMethodsListWithAnnotation(instance.getClass(), QueryHandler.class);
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

    public static void updateReadModelCacheField(Object readModel, InMemoryCacheProcessor inMemoryCacheProcessor) {
        Field field = getReadModelCacheField(readModel);
        try {
            FieldUtils.writeField(field, readModel, inMemoryCacheProcessor.getAll(), true);
        } catch (IllegalAccessException e) {
            log.error("Unable to write to the field annotated with the @DewdropCache on the ReadModel: {}", readModel.getClass()
                .getName(), e);
        }
    }
}
