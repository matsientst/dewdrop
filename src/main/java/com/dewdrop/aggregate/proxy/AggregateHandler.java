package com.dewdrop.aggregate.proxy;

import java.lang.reflect.Method;
import javassist.util.proxy.MethodHandler;

public class AggregateHandler<T> implements MethodHandler {

    @Override
    public Object invoke(Object self, Method overridden, Method forwarder, Object[] args) throws Throwable {
        System.out.println("do something " + overridden.getName());
        return forwarder.invoke(self, args);
    }
}
