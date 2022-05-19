package com.dewdrop.utils;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.command.CommandHandler;
import com.dewdrop.structure.api.Command;
import com.dewdrop.structure.api.Event;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CommandUtils {
    public static Optional<List<Event>> executeCommand(Object target, Method commandHandlerMethod, Command command, AggregateRoot aggregateRoot) {

        Class<?>[] parameterTypes = commandHandlerMethod.getParameterTypes();
        if (parameterTypes.length > 1) {
            return DewdropReflectionUtils.callMethod(target, commandHandlerMethod.getName(), command, aggregateRoot.getTarget());
        }

        return DewdropReflectionUtils.callMethod(target, commandHandlerMethod.getName(), command);
    }

    public static Class<?> getAggregateRootClassFromCommandHandlerMethod(Method commandHandlerMethod) {
        CommandHandler annotation = commandHandlerMethod.getAnnotation(CommandHandler.class);

        if (annotation.value() == void.class) {
            return commandHandlerMethod.getDeclaringClass();
        }

        return annotation.value();
    }

    public static Set<Method> getCommandHandlerMethods() {
        return AnnotationReflection.getAnnotatedMethods(CommandHandler.class);
    }
}
