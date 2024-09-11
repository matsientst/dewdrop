package events.dewdrop.utils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

import events.dewdrop.aggregate.AggregateRoot;
import events.dewdrop.api.result.Result;
import events.dewdrop.command.CommandHandler;
import events.dewdrop.structure.api.Command;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.reflect.MethodUtils;

@Log4j2
public class CommandHandlerUtils {
    private CommandHandlerUtils() {}

    private static final String COMMAND_HANDLER = CommandHandler.class.getSimpleName();

    public static <T> Result<T> executeCommand(Method commandHandlerMethod, Command command, AggregateRoot aggregateRoot) {
        requireNonNull(commandHandlerMethod, "We must have a method decorated with @" + COMMAND_HANDLER);
        requireNonNull(command, "We must have a command to pass to the @" + COMMAND_HANDLER);

        try {
            Object instance = aggregateRoot.getTarget();
            return executeCommand(instance, commandHandlerMethod, command, aggregateRoot);
        } catch (IllegalArgumentException e) {
            log.error("We were unable to call the command handler on {} - message: {}", commandHandlerMethod.getDeclaringClass().getSimpleName(), e.getMessage(), e);
            Result.of(e);
        }

        return Result.empty();
    }

    public static <T> Result<T> executeCommand(Object target, Method commandHandlerMethod, Command command, AggregateRoot aggregateRoot) {
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

    public static List<Method> getCommandHandlersForAggregateRoot(Class<?> aggregateRootClass) {
        List<Method> methodsListWithAnnotation = MethodUtils.getMethodsListWithAnnotation(aggregateRootClass, CommandHandler.class);
        return methodsListWithAnnotation;
    }

    public static Set<Method> getCommandHandlerMethods() {
        return DewdropAnnotationUtils.getAnnotatedMethods(CommandHandler.class);
    }
}
