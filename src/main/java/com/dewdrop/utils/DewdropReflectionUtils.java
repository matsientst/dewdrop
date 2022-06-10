package com.dewdrop.utils;

import com.dewdrop.structure.api.Message;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

@Log4j2
public class DewdropReflectionUtils {
    private DewdropReflectionUtils() {}

    public static boolean hasField(Object message, String name) {
        Field hasField = FieldUtils.getField(message.getClass(), name, true);
        return hasField != null;
    }

    public static <T> Optional<T> readFieldValue(Object instance, String name) {
        Field field = FieldUtils.getField(instance.getClass(), name, true);

        if (field != null) {
            T result = (T) DewdropReflectionUtils.readFieldValue(field, instance);
            return Optional.of(result);
        }
        return Optional.empty();
    }

    public static <T> T readFieldValue(Field field, Object instance) {
        try {
            return (T) FieldUtils.readField(field, instance, true);
        } catch (IllegalAccessException e) {
            log.error("Unable to call field: {} on the target:{}", field, instance);
            return null;
        }
    }

    public static <T> Optional<T> callMethod(Object object, String method, Object... args) {
        try {
            T result = (T) MethodUtils.invokeMethod(object, true, method, args);
            if (result != null) { return Optional.of(result); }
        } catch (IllegalArgumentException | InvocationTargetException e) {
            log.error("Unable to invoke {} on {} with args:{} - message: {}", method, object.getClass().getSimpleName(), args, e.getMessage(), e);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            log.error("We were unable to find the method:{}() on {} with args:{} - message: {}", method, object.getClass().getSimpleName(), args, e.getMessage(), e);
        }
        return Optional.empty();
    }

    public static <R> Optional<R> createInstance(Class<?> clazz) {
        try {
            R instance = (R) ConstructorUtils.invokeConstructor(clazz);
            return Optional.of(instance);
        } catch (InstantiationException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            log.error("Unable to create instance of {} - message: {}", clazz.getSimpleName(), e.getMessage(), e);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            log.error("Unable to create instance of {} - Are you missing an empty constructor?", clazz.getSimpleName(), e.getMessage(), e);
        }
        return Optional.empty();
    }

    public static <T extends Message> boolean hasAnyField(T message, List<String> primaryCacheKeyName) {
        for (String name : primaryCacheKeyName) {
            if (hasField(message, name)) { return true; }
        }
        return false;
    }
}
