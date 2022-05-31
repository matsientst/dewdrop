package com.dewdrop.utils;

import static java.util.Objects.requireNonNull;

import com.dewdrop.api.result.Result;
import com.dewdrop.read.readmodel.query.QueryHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class QueryHandlerUtils {
    private QueryHandlerUtils() {}

    public static <T, R> Result<R> callQueryHandler(Object target, T query) {
        requireNonNull(target, "Target ReadModel is required");
        requireNonNull(query, "A Query is required");

        Optional<Method> targetMethod = getMethodForQuery(target.getClass(), query);
        if (targetMethod.isEmpty()) {
            log.debug("Unable to find method annotated with @QueryHandler with method signature query({} query) on target class: {}", query.getClass()
                .getSimpleName(), query.getClass()
                .getSimpleName());
            return Result.empty();
        }

        try {
            R invoke = (R) targetMethod.get()
                .invoke(target, query);
            if (invoke instanceof Result) {
                return (Result<R>) invoke;
            }
            return invoke == null ? Result.empty() : Result.of(invoke);

        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            log.error("Unable to invoke method annotated with @QueryHandler with method signature query({} query) on {} - Make sure the method exists", query.getClass()
                .getSimpleName(), target.getClass()
                .getSimpleName(), e);
        }
        return Result.empty();
    }

    public static <T> Optional<Method> getMethodForQuery(Class<?> target, T query) {
        Set<Method> methods = DewdropAnnotationUtils.getAnnotatedMethods(target, QueryHandler.class);
        return methods.stream()
            .filter(method -> method.getParameterTypes()[0].equals(query.getClass()))
            .findAny();
    }
}
