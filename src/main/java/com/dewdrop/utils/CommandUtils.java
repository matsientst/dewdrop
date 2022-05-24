package com.dewdrop.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.command.CommandHandler;
import com.dewdrop.structure.api.Command;
import com.dewdrop.structure.api.Event;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CommandUtils {
    public static Optional<List<Event>> executeCommand(Object target, Method commandHandlerMethod, Command command, AggregateRoot aggregateRoot) {
        String simpleName = CommandHandler.class.getSimpleName();
        requireNonNull(target, "We must have a target to call");
        requireNonNull(commandHandlerMethod, "We must have a method decorated with @" + simpleName);
        requireNonNull(command, "We must have a command to pass to the @" + simpleName);

        Class<?>[] parameterTypes = commandHandlerMethod.getParameterTypes();

        if (parameterTypes.length > 1) {
            log.debug("Calling method decorated with @{} {}.{}({} command, {} aggregateRoot)", simpleName, commandHandlerMethod.getDeclaringClass().getSimpleName(), commandHandlerMethod.getName(), parameterTypes[0].getSimpleName(),
                            parameterTypes[1].getSimpleName());
            return DewdropReflectionUtils.callMethod(target, commandHandlerMethod.getName(), command, aggregateRoot.getTarget());
        }

        log.debug("Calling method decorated with @{} {}.{}({} command)", simpleName, commandHandlerMethod.getDeclaringClass().getSimpleName(), commandHandlerMethod.getName(), parameterTypes[0].getSimpleName());
        return DewdropReflectionUtils.callMethod(target, commandHandlerMethod.getName(), command);
    }

    public static Class<?> getAggregateRootClassFromCommandHandlerMethod(Method commandHandlerMethod) {
        CommandHandler annotation = commandHandlerMethod.getAnnotation(CommandHandler.class);

        if (isNull(annotation)) {
            String parameters = Arrays.stream(commandHandlerMethod.getParameters()).map(p -> p.getType().getSimpleName()).collect(Collectors.toList()).toString();
            log.warn("No CommandHandler has been annotated for: " + commandHandlerMethod.getDeclaringClass() + "\n  Method Name: " + commandHandlerMethod.getName() + "\n  Parameters: " + parameters);
          return null;
        }

        if (annotation.value() == void.class) {
            return commandHandlerMethod.getDeclaringClass();
        }

        return annotation.value();
    }

    public static Set<Method> getCommandHandlerMethods() {
        return AnnotationReflection.getAnnotatedMethods(CommandHandler.class);
    }
}
