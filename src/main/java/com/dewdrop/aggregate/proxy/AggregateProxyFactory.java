package com.dewdrop.aggregate.proxy;

import static java.util.Objects.isNull;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.utils.CommandUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AggregateProxyFactory {

    public static <T> Optional<T> create(Class<?> classToProxy) {
        try {
            return Optional.of((T) new AggregateRoot(classToProxy.getDeclaredConstructor()
                .newInstance(), classToProxy.getName()));
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            log.error("Failed to assign AggregateRoot superclass", e);
            return Optional.empty();
        }
    }

    public static Optional<AggregateRoot> createFromCommandHandlerMethod(Method commandHandlerMethod) {
        Class<?> aggregateClass = CommandUtils.getAggregateRootClassFromCommandHandlerMethod(commandHandlerMethod);

        if (isNull(aggregateClass)) {
            return Optional.empty();
        }

        return create(aggregateClass);
    }
}

