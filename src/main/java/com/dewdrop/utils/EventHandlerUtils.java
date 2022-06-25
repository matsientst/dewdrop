package com.dewdrop.utils;

import static java.util.stream.Collectors.toList;

import com.dewdrop.read.readmodel.ReadModel;
import com.dewdrop.read.readmodel.annotation.EventHandler;
import com.dewdrop.read.readmodel.annotation.OnEvent;
import com.dewdrop.read.readmodel.cache.InMemoryCacheProcessor;
import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.api.Message;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class EventHandlerUtils {
    private EventHandlerUtils() {}

    public static List<Class<? extends Event>> getEventHandlers(ReadModel readModel) {
        Class<?> target;
        if (readModel.getInMemoryCacheProcessor().isPresent()) {
            InMemoryCacheProcessor processor = (InMemoryCacheProcessor) readModel.getInMemoryCacheProcessor().get();
            target = processor.getCachedStateObjectType();
        } else {
            target = readModel.getReadModel().getClass();
        }
        Set<Method> methods = DewdropAnnotationUtils.getAnnotatedMethods(target, EventHandler.class);
        return methods.stream().filter(method -> method.getParameterTypes().length > 0).filter(method -> {
            return Event.class.isAssignableFrom(method.getParameterTypes()[0]);
        }).map(method -> (Class<? extends Event>) method.getParameterTypes()[0]).collect(toList());
    }

    public static <T extends Message> void callEventHandler(Object target, T event) {
        callEventHandler(target, event, null);
    }

    public static <T extends Message, R> void callEventHandler(Object target, T event, R secondArg) {
        Optional<Method> targetMethod = getEventHandlerMethod(target.getClass(), event);
        callEventMethod(target, event, secondArg, targetMethod, EventHandler.class);
    }

    private static <T extends Message, R> void callEventMethod(Object target, T event, R secondArg, Optional<Method> targetMethod, Class<? extends Annotation> annotation) {
        if (targetMethod.isEmpty()) {
            log.debug("Unable to find method annotated with @{} with method signature on({} event) on target class: {}", annotation.getSimpleName(), event.getClass().getSimpleName(), target.getClass().getSimpleName());
            return;
        }

        try {
            Method method = targetMethod.get();
            if (method.getParameterTypes().length > 1) {
                method.invoke(target, event, secondArg);
            } else {
                method.invoke(target, event);
            }

        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            log.error("Unable to invoke method annotated with @{} with method signature on({} event) on {} - Make sure the method exists", annotation.getSimpleName(), event.getClass().getSimpleName(), target.getClass().getSimpleName(), e);
        }
    }

    public static <T extends Message> Optional<Method> getEventHandlerMethod(Class target, T event) {
        return getMethodsWithAnnotationForEvent(target, event, EventHandler.class);
    }

    public static <T extends Message> Optional<Method> getOnEventMethod(Class target, T event) {
        return getMethodsWithAnnotationForEvent(target, event, OnEvent.class);
    }

    public static <T extends Message> Optional<Method> getMethodsWithAnnotationForEvent(Class target, T event, Class<? extends Annotation> annotation) {
        Set<Method> methods = DewdropAnnotationUtils.getAnnotatedMethods(target, annotation);
        return methods.stream().filter(method -> method.getParameterTypes()[0].equals(event.getClass())).findAny();
    }

    public static <T extends Event> void callOnEvent(Object target, T event) {
        Optional<Method> targetMethod = getOnEventMethod(target.getClass(), event);
        callEventMethod(target, event, null, targetMethod, OnEvent.class);
    }
}
