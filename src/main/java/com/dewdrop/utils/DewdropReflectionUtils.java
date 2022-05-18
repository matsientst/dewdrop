package com.dewdrop.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.reflect.MethodUtils;

@Log4j2
public class DewdropReflectionUtils {

    public static <T> Optional<T> callMethod(Object object, String method, Object... args) {
        try {
            T result = (T) MethodUtils.invokeMethod(object, true, method, args);
            if (result != null) {
                return Optional.of(result);
            }
        } catch (IllegalArgumentException | InvocationTargetException e) {
            log.error("Unable to invoke {} on {} with args:{} - message: {}", method, object.getClass()
                .getSimpleName(), args, e.getMessage(), e);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            log.error("We were unable to find the method:{}() on {} with args:{} - message: {}", method, object.getClass()
                .getSimpleName(), args, e.getMessage(), e);
        }
        return Optional.empty();
    }

    public static <R> Optional<R> createInstance(Class<?> cachedStateObjectType) {
        try {
            Constructor<?> constructor = cachedStateObjectType.getConstructor();
            constructor.setAccessible(true);
            R instance = (R) constructor
                .newInstance();
            return Optional.of(instance);
        } catch (InstantiationException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            log.error("Unable to create instance of {} - message: {}", cachedStateObjectType.getSimpleName(), e.getMessage(), e);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            log.error("Unable to create instance of {} - Are you missing an empty constructor?", cachedStateObjectType.getSimpleName(), e.getMessage(), e);
        }
        return Optional.empty();
    }
}
