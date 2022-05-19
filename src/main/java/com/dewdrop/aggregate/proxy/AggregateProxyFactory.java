package com.dewdrop.aggregate.proxy;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.utils.CommandUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AggregateProxyFactory {
    public static <T> Optional<T> create(Class<?> classToProxy) {
//        ClassPool pool = ClassPool.getDefault();
        try {
//            ProxyFactory factory = new ProxyFactory();
//            factory.setSuperclass(classToProxy);
//
//            MethodHandler handler = new AggregateHandler<>();
//            Object instance = factory.create(null, null, handler);


            return Optional.of((T) new AggregateRoot(classToProxy.getDeclaredConstructor().newInstance(), classToProxy.getName()));
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            log.error("Failed to assign AggregateRoot superclass", e);
            return Optional.empty();
        }
    }

    public static Optional<AggregateRoot> createFromCommandHandlerMethod(Method commandHandlerMethod) {
        Class<?> aggregateClass = CommandUtils.getAggregateRootClassFromCommandHandlerMethod(commandHandlerMethod);

        return create(aggregateClass);
    }
}

