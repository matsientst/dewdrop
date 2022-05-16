package com.dewdropper.aggregate.proxy;

import com.dewdropper.aggregate.AggregateRoot;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import javassist.ClassPool;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AggregateProxyFactory {
    // Having trouble creating an actual proxy. Javassist is throwing a proxy classcastexception and spring is
    //Could not generate CGLIB subclass of class com.dewdropper.TestAccountAggregate: Common causes of this problem include using a final class or a non-visible class; nested exception is org.springframework.cglib.core.CodeGenerationException: java.lang.NoSuchMethodException-->com.dewdropper.TestAccountAggregate$$EnhancerBySpringCGLIB$$8d9162a.CGLIB$SET_THREAD_CALLBACKS([Lorg.springframework.cglib.proxy.Callback;)
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
}

