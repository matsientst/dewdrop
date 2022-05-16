package com.dewdropper.command;

import com.dewdropper.aggregate.AggregateRoot;
import com.dewdropper.aggregate.proxy.AggregateProxyFactory;
import com.dewdropper.api.result.Result;
import com.dewdropper.streamstore.repository.StreamStoreRepository;
import com.dewdropper.structure.api.Command;
import com.dewdropper.structure.api.Event;
import com.dewdropper.utils.AggregateIdUtils;
import com.dewdropper.utils.ReflectionUtils;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

@Log4j2
public abstract class CommandHandlerMapper implements CommandMapper {
    private StreamStoreRepository streamStoreRepository;

    public CommandHandlerMapper() {}

    public CommandHandlerMapper(StreamStoreRepository streamStoreRepository) {
        this.streamStoreRepository = streamStoreRepository;
        init();
    }

    private List<Class<?>> commandHandlers;

    public void init() {
        commandHandlers = new ArrayList<>();

        Set<Class<?>> aggregates = ReflectionUtils.getAnnotatedClasses(CommandHandler.class);

        aggregates.forEach(aggregate -> {
            commandHandlers.add(aggregate);
        });

        if (CollectionUtils.isEmpty(commandHandlers)) {
            log.error("No AggregateRoots found - Make sure to annotate your aggregateRoots with @Aggregate");
        }
    }

    public Map<Class<?>, List<Object>> getCommandHandlersThatSupportCommand(Command command) {
        Map<Class<?>, List<Object>> result = new java.util.HashMap<>();
        // add default command mapper which maps to the aggregate
        // custom mapper could target a service or a function on how to handle the command
        commandHandlers.forEach(commandHandler -> {
            Method handler = MethodUtils.getMatchingAccessibleMethod(commandHandler, "handle", command.getClass());
            if (handler != null) {
                Class<?> value = handler.getAnnotation(CommandHandler.class)
                    .value();
                result.computeIfAbsent(value, k -> new ArrayList<>())
                    .add(commandHandler);
            }
        });

        if (MapUtils.isEmpty(result)) {
            log.error("No CommandHandlers found that have a method handle({} command)", command.getClass()
                .getSimpleName());
        }

        return result;
    }

    public Result<List<Object>> onCommand(Command command) {
        Map<Class<?>, List<Object>> aggregateRootToHandlers = getCommandHandlersThatSupportCommand(command);

        if (aggregateRootToHandlers.isEmpty()) {
            return Result.of(new ArrayList<>());
        }

        List<Object> results = new ArrayList<>();
        Set<Class<?>> aggregates = aggregateRootToHandlers.keySet();
        aggregates.forEach(aggregate -> {
            Optional<AggregateRoot> optAggregateRoot = AggregateProxyFactory.create(aggregate);
            if (optAggregateRoot.isPresent()) {
                AggregateRoot aggregateRoot = optAggregateRoot.get();

                Optional<UUID> aggregateId = AggregateIdUtils.getAggregateId(command);
                if (aggregateId.isPresent()) {
                    aggregateRoot = streamStoreRepository.getById(aggregateId.get(), aggregateRoot, Integer.MAX_VALUE, command)
                        .orElse(aggregateRoot);
                }
                List<Object> handlers = aggregateRootToHandlers.get(aggregate);
                final AggregateRoot finalAggregateRoot = aggregateRoot;
                handlers.forEach(handler -> {
                    try {
                        Optional<Event> result = ReflectionUtils.callMethod(handler, "handle", command);
                        if (result.isPresent()) {
                            finalAggregateRoot.raise(result.get());
                        }

                    } catch (Exception e) {
                        log.error("Error handling command", e);
                    }
                });
                streamStoreRepository.save(finalAggregateRoot);
                results.add(finalAggregateRoot.getTarget());
            }
        });
        return Result.of(results);
    }
}
