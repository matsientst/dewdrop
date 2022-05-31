package com.dewdrop.utils;

import static java.util.stream.Collectors.toList;

import com.dewdrop.read.readmodel.annotation.EventHandler;
import com.dewdrop.structure.api.Message;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class EventHandlerUtils {
    private EventHandlerUtils() {}

    public static List<Class<?>> getFirstParameterForEventHandlerMethods(Class<?> target) {
        Set<Method> methods = DewdropAnnotationUtils.getAnnotatedMethods(target, EventHandler.class);
        return methods.stream()
            .filter(method -> method.getParameterTypes().length > 0)
            .map(method -> method.getParameterTypes()[0])
            .collect(toList());
    }

    public static <T extends Message> void callEventHandler(Object target, T event) {
        callEventHandler(target, event, null);
    }

    public static <T extends Message, R> void callEventHandler(Object target, T event, R secondArg) {
        Optional<Method> targetMethod = getMethodForEvent(target.getClass(), event);
        if (targetMethod.isEmpty()) {
            log.debug("Unable to find method annotated with @EventHandler with method signature on({} event) on target class: {}", event.getClass()
                .getSimpleName(), target.getClass()
                .getSimpleName());
            return;
        }

        try {
            Method method = targetMethod.get();
            if (method.getParameterTypes().length > 1) {
                method
                    .invoke(target, event, secondArg);
            } else {
                method
                    .invoke(target, event);
            }

        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            log.error("Unable to invoke method annotated with @EventHandler with method signature on({} event) on {} - Make sure the method exists", event.getClass()
                .getSimpleName(), target.getClass()
                .getSimpleName(), e);
        }
    }

    public static <T extends Message> Optional<Method> getMethodForEvent(Class target, T event) {
        Set<Method> methods = DewdropAnnotationUtils.getAnnotatedMethods(target, EventHandler.class);
        return methods.stream()
            .filter(method -> method.getParameterTypes()[0].equals(event.getClass()))
            .findAny();
    }
}
