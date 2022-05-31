package com.dewdrop.command;

import com.dewdrop.streamstore.repository.StreamStoreRepository;
import com.dewdrop.structure.api.Command;
import com.dewdrop.utils.CommandHandlerUtils;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

@Log4j2
public class CommandHandlerMapper extends AbstractCommandHandlerMapper {
    Map<Class<?>, Method> commandHandlerToMethod = new java.util.HashMap<>();

    public CommandHandlerMapper() {}

    public void init(StreamStoreRepository streamStoreRepository) {
        log.debug("Finding all commandHandlerMethods annotated with @CommandHandler");
        super.construct(streamStoreRepository);


        Set<Method> commandHandlerMethods = CommandHandlerUtils.getCommandHandlerMethods();
        if (CollectionUtils.isEmpty(commandHandlerMethods)) {
            log.error("No command handlers found - Make sure to annotate your handlers with @CommandHandler - If in your AggregateRoot it should be handle(MyCustomCommand command) or if in a service handle(MyCustomCommand command, MyAggregateRoot aggregateRoot)");
        }

        commandHandlerMethods.forEach(commandHandlerMethod -> {
            Class<?> commandClass = commandHandlerMethod.getParameterTypes()[0];

            if (commandHandlerToMethod.containsKey(commandClass)) {

                log.error("InvalidState - Duplicate @CommandHandler handle({} command) found in {}", commandClass.getSimpleName(), commandHandlerMethod.getDeclaringClass());
                return;
            }

            log.info("Registering @CommandHandler for {} to be handled by {}", commandClass.getSimpleName(), commandHandlerMethod.getDeclaringClass().getSimpleName());
            commandHandlerToMethod.put(commandClass, commandHandlerMethod);
        });
    }

    public Optional<Method> getCommandHandlersThatSupportCommand(Command command) {
        Method method = commandHandlerToMethod.get(command.getClass());
        if (method == null) { return Optional.empty(); }
        return Optional.of(method);
    }
}
