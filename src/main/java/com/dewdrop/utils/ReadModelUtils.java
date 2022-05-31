package com.dewdrop.utils;

import static java.util.Objects.requireNonNull;

import com.dewdrop.read.readmodel.annotation.ReadModel;
import com.dewdrop.read.readmodel.query.QueryHandler;
import com.dewdrop.structure.api.Message;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

@Log4j2
public class ReadModelUtils {
    ReadModelUtils() {}

    private static ReadModelUtils instance;
    private final List<Class<?>> READ_MODEL_CACHE = new ArrayList<>();

    public static List<Class<?>> getAnnotatedReadModels() {
        ReadModelUtils local = getInstance();
        List<Class<?>> READ_MODEL_CACHE = local.READ_MODEL_CACHE;
        if (!READ_MODEL_CACHE.isEmpty()) { return READ_MODEL_CACHE; }

        Set<Class<?>> readModelClasses = AnnotationReflection.getAnnotatedClasses(ReadModel.class);

        READ_MODEL_CACHE.addAll(readModelClasses);

        if (CollectionUtils.isEmpty(READ_MODEL_CACHE)) {
            log.error("No classes annotated with @ReadModel - Without a ReadModel you cannot query");
        }
        return READ_MODEL_CACHE;
    }

    public static void processOnEvent(Object target, Message message) {
        DewdropReflectionUtils.callMethod(target, "on", message);
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
}
