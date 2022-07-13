package events.dewdrop.utils;

import static java.util.Objects.requireNonNull;

import events.dewdrop.api.result.Result;
import events.dewdrop.read.readmodel.ReadModelWrapper;
import events.dewdrop.read.readmodel.query.QueryHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class QueryHandlerUtils {
    private QueryHandlerUtils() {}

    public static <T, R> Result<R> callQueryHandler(ReadModelWrapper readModelWrapper, T query) {
        requireNonNull(readModelWrapper, "ReadModelWrapper is required");
        requireNonNull(query, "A Query is required");

        Optional<Method> targetMethod = getMethodForQuery(readModelWrapper.getOriginalReadModelClass(), query);
        if (targetMethod.isEmpty()) {
            log.info("Unable to find method annotated with @QueryHandler with method signature query({} query) on target class: {}", query.getClass().getSimpleName(), query.getClass().getSimpleName());
            return Result.empty();
        }

        Optional<Method> method = DewdropReflectionUtils.getMatchingMethod(targetMethod.get(), readModelWrapper.getReadModel());
        if (method.isEmpty()) {
            log.info("Unable to find method annotated with @QueryHandler with method signature query({} query) on target class: {}", query.getClass().getSimpleName(), readModelWrapper.getOriginalReadModelClass().getSimpleName());
            return Result.empty();
        }
        try {
            R invoke = (R) method.get().invoke(readModelWrapper.getReadModel(), query);
            if (invoke instanceof Result) { return (Result<R>) invoke; }
            return invoke == null ? Result.empty() : Result.of(invoke);

        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            log.error("Unable to invoke method annotated with @QueryHandler with method signature query({} query) on {} - Make sure the method exists", query.getClass().getSimpleName(), readModelWrapper.getOriginalReadModelClass().getSimpleName(),
                            e);
        }
        return Result.empty();
    }

    public static <T> Optional<Method> getMethodForQuery(Class<?> target, T query) {
        Set<Method> methods = DewdropAnnotationUtils.getAnnotatedMethods(target, QueryHandler.class);
        return methods.stream().filter(method -> {
            if (method.getParameterTypes().length == 0) { return false; }
            return method.getParameterTypes()[0].equals(query.getClass());
        }).findAny();
    }
}
