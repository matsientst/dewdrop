package com.dewdrop.aggregate;

import com.dewdrop.aggregate.proxy.AggregateProxyFactory;
import com.dewdrop.api.result.Result;
import com.dewdrop.command.CommandMapper;
import com.dewdrop.streamstore.repository.StreamStoreRepository;
import com.dewdrop.structure.api.Command;
import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.events.CorrelationCausation;
import com.dewdrop.utils.AggregateIdUtils;
import com.dewdrop.utils.AssignCorrelationAndCausation;
import com.dewdrop.utils.CommandUtils;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AggregateStateOrchestrator {
    private CommandMapper commandMapper;
    private StreamStoreRepository streamStoreRepository;

    public AggregateStateOrchestrator() {}

    public AggregateStateOrchestrator(CommandMapper commandMapper, StreamStoreRepository streamStoreRepository) {
        this.commandMapper = commandMapper;
        this.streamStoreRepository = streamStoreRepository;
    }

    public Result<Object> onCommand(Command command) {
        Optional<Method> commandHandlerMethod = commandMapper.getCommandHandlersThatSupportCommand(command);

        if (commandHandlerMethod.isEmpty()) {
            return Result.of(new ArrayList<>());
        }

//        command = AssignCorrelationAndCausation.firstCommand(command);
        log.info("Handling command correlationId:{}, causationId:{}, messageId:{}", command.getCorrelationId(), command.getCausationId(), command.getMessageId());
        return processCommand(command, commandHandlerMethod.get());
    }

    public Result<Object> onSubsequentCommand(Command command, CorrelationCausation previous) {
        Optional<Method> commandHandlerMethod = commandMapper.getCommandHandlersThatSupportCommand(command);

        if (commandHandlerMethod.isEmpty()) {
            return Result.of(new ArrayList<>());
        }

        command = AssignCorrelationAndCausation.assignTo(previous, command);
        return processCommand(command, commandHandlerMethod.get());
    }

    private Result<Object> processCommand(Command command, Method commandHandlerMethod) {
        Optional<AggregateRoot> optAggregateRoot = AggregateProxyFactory.createFromCommandHandlerMethod(commandHandlerMethod);

        if (optAggregateRoot.isPresent()) {
            AggregateRoot aggregateRoot = optAggregateRoot.get();
            aggregateRoot = getById(command, aggregateRoot);
            aggregateRoot = executeCommand(command, commandHandlerMethod, aggregateRoot);
            aggregateRoot = save(aggregateRoot);
            return Result.of(aggregateRoot.getTarget());
        }

        return Result.empty();
    }

    private AggregateRoot save(AggregateRoot aggregateRoot) {
        streamStoreRepository.save(aggregateRoot);
        return aggregateRoot;
    }

    private AggregateRoot executeCommand(Command command, Method handler, AggregateRoot aggregateRoot) {
        try {
            Object instance = handler.getDeclaringClass()
                .getDeclaredConstructor()
                .newInstance();
            Optional<List<Event>> result = CommandUtils.executeCommand(instance, handler, command, aggregateRoot);
            if (result.isPresent()) {
                for (Event event : result.get()) {
                    aggregateRoot.raise(event);
                }
            }

        } catch (Exception e) {
            log.error("Error handling command", e);
        }
        return aggregateRoot;
    }

    private AggregateRoot getById(Command command, AggregateRoot aggregateRoot) {
        Optional<UUID> aggregateId = AggregateIdUtils.getAggregateId(command);
        if (aggregateId.isPresent()) {
            aggregateRoot = streamStoreRepository.getById(aggregateId.get(), aggregateRoot, Integer.MAX_VALUE, command);
        }
        return aggregateRoot;
    }
}
