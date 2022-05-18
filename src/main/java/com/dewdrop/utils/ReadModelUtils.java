package com.dewdrop.utils;

import static java.util.Objects.requireNonNull;

import com.dewdrop.read.readmodel.CacheableCategoryReadModel;
import com.dewdrop.read.readmodel.ReadModel;
import com.dewdrop.read.readmodel.query.QueryHandler;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.serialize.EventSerializer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

@Log4j2
public class ReadModelUtils {
    private static final List<Class<?>> READ_MODEL_CACHE = new ArrayList<>();

    public static List<Class<?>> getAnnotatedReadModels() {
        if (!READ_MODEL_CACHE.isEmpty()) {
            return READ_MODEL_CACHE;
        }

        Set<Class<?>> readModelClasses = AnnotationReflection.getAnnotatedClasses(ReadModel.class);

        READ_MODEL_CACHE.addAll(readModelClasses);

        if (CollectionUtils.isEmpty(READ_MODEL_CACHE)) {
            log.error("No ReadModelClasses found - Make sure to annotate your ReadModels with @ReadModel");
        }
        return READ_MODEL_CACHE;
    }

    public static void processOnEvent(Object target, Message message) {
        DewdropReflectionUtils.callMethod(target, "on", message);
    }

    public static Optional<CacheableCategoryReadModel> constructReadModel(Class<?> target, StreamStore streamStore, EventSerializer eventSerializer) {
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

        CacheableCategoryReadModel<Message, Object> value = contruct(instance, streamStore, eventSerializer).get();
        if (value != null) {return Optional.ofNullable(value);}

        return Optional.empty();
    }

    public static <T extends Message> Supplier<CacheableCategoryReadModel<Message, Object>> contruct(java.lang.Object target, StreamStore streamStore, EventSerializer eventSerializer) {
        return () -> {
            com.dewdrop.read.readmodel.ReadModel annotation = target.getClass()
                .getAnnotation(com.dewdrop.read.readmodel.ReadModel.class);
            Class<?> rootEvent = annotation
                .rootEvent();
            Class<?> aggregateClass = annotation.aggregateClass();
            Class<?> resultClass = annotation.resultClass();
            CacheableCategoryReadModel<Message, Object> readModel = new CacheableCategoryReadModel<>(target, rootEvent, resultClass, aggregateClass, streamStore, eventSerializer);
            return readModel;
        };
    }

    public static List<Method> getQueryMethods(Object instance) {
        requireNonNull(instance, "Object is required");

        return MethodUtils.getMethodsListWithAnnotation(instance.getClass(), QueryHandler.class);
    }
}
