package com.dewdrop.utils;

import static java.util.Objects.requireNonNull;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.command.CommandHandler;
import com.dewdrop.structure.api.Command;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CommandHandlerUtils {
    private CommandHandlerUtils() {}

    private static final String COMMAND_HANDLER = CommandHandler.class.getSimpleName();

    public static <T> Optional<T> executeCommand(Method commandHandlerMethod, Command command, AggregateRoot aggregateRoot) {
        requireNonNull(commandHandlerMethod, "We must have a method decorated with @" + COMMAND_HANDLER);
        requireNonNull(command, "We must have a command to pass to the @" + COMMAND_HANDLER);
        try {
            Object instance = commandHandlerMethod.getDeclaringClass().getDeclaredConstructor().newInstance();
            return executeCommand(instance, commandHandlerMethod, command, aggregateRoot);
        } catch (IllegalArgumentException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            log.error("We were unable to call an empty constructor on {} - message: {}", commandHandlerMethod.getDeclaringClass().getSimpleName(), e.getMessage(), e);
        }
        return Optional.empty();
    }

    public static <T> Optional<T> executeCommand(Object target, Method commandHandlerMethod, Command command, AggregateRoot aggregateRoot) {
        requireNonNull(target, "We must have a target to call");
        requireNonNull(commandHandlerMethod, "We must have a method decorated with @" + COMMAND_HANDLER);
        requireNonNull(command, "We must have a command to pass to the @" + COMMAND_HANDLER);

        Class<?>[] parameterTypes = commandHandlerMethod.getParameterTypes();

        if (parameterTypes.length > 1) {
            log.debug("Calling method decorated with @{} {}.{}({} command, {} aggregateRoot)", COMMAND_HANDLER, commandHandlerMethod.getDeclaringClass().getSimpleName(), commandHandlerMethod.getName(), parameterTypes[0].getSimpleName(),
                            parameterTypes[1].getSimpleName());
            return DewdropReflectionUtils.callMethod(target, commandHandlerMethod.getName(), command, aggregateRoot.getTarget());
        }

        log.debug("Calling method decorated with @{} {}.{}({} command)", COMMAND_HANDLER, commandHandlerMethod.getDeclaringClass().getSimpleName(), commandHandlerMethod.getName(), parameterTypes[0].getSimpleName());
        return DewdropReflectionUtils.callMethod(target, commandHandlerMethod.getName(), command);
    }

    public static Class<?> getAggregateRootClassFromCommandHandlerMethod(Method commandHandlerMethod) {
        CommandHandler annotation = commandHandlerMethod.getAnnotation(CommandHandler.class);

        if (annotation.value() == void.class) { return commandHandlerMethod.getDeclaringClass(); }

        return annotation.value();
    }

    public static Set<Method> getCommandHandlerMethods() {
        return DewdropAnnotationUtils.getAnnotatedMethods(CommandHandler.class);
    }
}
