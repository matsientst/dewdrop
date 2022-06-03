package com.dewdrop.aggregate;

import com.dewdrop.api.result.Result;
import com.dewdrop.command.CommandMapper;
import com.dewdrop.read.StreamDetails;
import com.dewdrop.read.readmodel.StreamDetailsFactory;
import com.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import com.dewdrop.streamstore.repository.StreamStoreRepository;
import com.dewdrop.structure.api.Command;
import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.events.CorrelationCausation;
import com.dewdrop.utils.AggregateIdUtils;
import com.dewdrop.utils.AggregateUtils;
import com.dewdrop.utils.AssignCorrelationAndCausation;
import com.dewdrop.utils.CommandHandlerUtils;
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
    private StreamDetailsFactory streamDetailsFactory;

    public AggregateStateOrchestrator() {}

    public AggregateStateOrchestrator(CommandMapper commandMapper, StreamStoreRepository streamStoreRepository, StreamDetailsFactory streamDetailsFactory) {
        this.commandMapper = commandMapper;
        this.streamStoreRepository = streamStoreRepository;
        this.streamDetailsFactory = streamDetailsFactory;
    }

    public Result<Object> executeCommand(Command command) {
        Optional<Method> commandHandlerMethod = commandMapper.getCommandHandlersThatSupportCommand(command);

        if (commandHandlerMethod.isEmpty()) { return Result.of(new ArrayList<>()); }

        return processCommand(command, commandHandlerMethod.get());
    }

    public Result<Object> executeSubsequentCommand(Command command, CorrelationCausation previous) {
        Optional<Method> commandHandlerMethod = commandMapper.getCommandHandlersThatSupportCommand(command);

        if (commandHandlerMethod.isEmpty()) { return Result.of(new ArrayList<>()); }

        command = AssignCorrelationAndCausation.assignTo(previous, command);
        return processCommand(command, commandHandlerMethod.get());
    }

    private Result<Object> processCommand(Command command, Method commandHandlerMethod) {
        Optional<AggregateRoot> optAggregateRoot = AggregateUtils.createFromCommandHandlerMethod(commandHandlerMethod);

        if (optAggregateRoot.isPresent()) {
            AggregateRoot aggregateRoot = optAggregateRoot.get();
            log.debug("Processing command {}", command.getClass().getSimpleName());
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
            Object instance = handler.getDeclaringClass().getDeclaredConstructor().newInstance();
            Optional<?> result = CommandHandlerUtils.executeCommand(instance, handler, command, aggregateRoot);
            if (result.isPresent()) {
                if (result.get() instanceof List) {
                    for (Event event : (List<Event>) result.get()) {
                        aggregateRoot.raise(event);
                    }
                } else {
                    aggregateRoot.raise((Event) result.get());
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
            StreamDetails streamDetails = streamDetailsFactory.fromAggregateRoot(aggregateRoot, aggregateId.get());
            StreamStoreGetByIDRequest request = StreamStoreGetByIDRequest.builder().streamDetails(streamDetails).aggregateRoot(aggregateRoot).id(aggregateId.get()).command(command).create();
            aggregateRoot = streamStoreRepository.getById(request);
        }
        return aggregateRoot;
    }
}
