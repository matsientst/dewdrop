package events.dewdrop.utils;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.extern.log4j.Log4j2;
import events.dewdrop.read.readmodel.ReadModel;
import events.dewdrop.read.readmodel.annotation.EventHandler;
import events.dewdrop.read.readmodel.annotation.OnEvent;
import events.dewdrop.read.readmodel.cache.InMemoryCacheProcessor;
import events.dewdrop.structure.api.Event;
import events.dewdrop.structure.api.Message;

@Log4j2
public class EventHandlerUtils {
    private EventHandlerUtils() {}

    public static List<Class<? extends Event>> getEventHandlers(ReadModel readModel) {
        if (readModel.getInMemoryCacheProcessor().isEmpty()) { return readModel.getReadModelWrapper().getSupportedEvents(); }

        InMemoryCacheProcessor processor = (InMemoryCacheProcessor) readModel.getInMemoryCacheProcessor().get();
        Class<?> target = processor.getCachedStateObjectType();

        Map<Class<? extends Event>, Method> eventToEventHandlerMethod = getEventToHandlerMethod(target, EventHandler.class);
        return eventToEventHandlerMethod.values().stream().map(method -> (Class<? extends Event>) method.getParameterTypes()[0]).collect(toList());
    }

    public static Map<Class<? extends Event>, Method> getEventToHandlerMethod(Class<?> readModelClass, Class<? extends Annotation> annotationClass) {
        return DewdropAnnotationUtils.getAnnotatedMethods(readModelClass, annotationClass).stream().filter(method -> {
            boolean noParameter = method.getParameterTypes().length > 0;
            if (!noParameter) {
                String methodName = method.getName();
                log.error("The method annotated with @EventHandler {}.{}() has no parameter and is invalid and cannot handle any events. Please add the event you want to handle like {}(ExampleEvent event) where ExampleEvent extends Event as the first parameter",
                                method.getDeclaringClass(), methodName, methodName);
            }
            return noParameter;
        }).filter(method -> {
            boolean isAssignable = Event.class.isAssignableFrom(method.getParameterTypes()[0]);
            if (!isAssignable) {
                String methodName = method.getName();
                log.error("The method annotated with @EventHandler {}.{}({} event) has a parameter that is not an event. Please add the event you want to handle like {}(ExampleEvent event) where ExampleEvent extends Event as the first parameter",
                                method.getDeclaringClass(), methodName, method.getParameterTypes()[0].getSimpleName(), methodName);
            }
            return isAssignable;
        }).collect(toMap(method -> (Class<? extends Event>) method.getParameterTypes()[0], Function.identity()));
    }

    public static Map<Class<? extends Event>, Method> getEventToEventHandlerMethod(Class<?> readModelClass) {
        return getEventToHandlerMethod(readModelClass, EventHandler.class);
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
            method.setAccessible(true);
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
}
