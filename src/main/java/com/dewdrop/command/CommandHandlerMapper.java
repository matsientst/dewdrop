package com.dewdrop.command;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.aggregate.proxy.AggregateProxyFactory;
import com.dewdrop.api.result.Result;
import com.dewdrop.streamstore.repository.StreamStoreRepository;
import com.dewdrop.structure.api.Command;
import com.dewdrop.structure.api.Event;
import com.dewdrop.utils.AggregateIdUtils;
import com.dewdrop.utils.AnnotationReflection;
import com.dewdrop.utils.DewdropReflectionUtils;
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

@Log4j2
public class CommandHandlerMapper extends AbstractCommandHandlerMapper {
    private List<Method> commandHandlers;

    public CommandHandlerMapper() {}

    public void init(StreamStoreRepository streamStoreRepository) {
        super.construct(streamStoreRepository);

        commandHandlers = new ArrayList<>();

        Set<Method> methods = AnnotationReflection.getAnnotatedMethods(CommandHandler.class);

        methods.forEach(method -> {
            commandHandlers.add(method);
        });

        if (CollectionUtils.isEmpty(commandHandlers)) {
            log.error("No AggregateRoots found - Make sure to annotate your aggregateRoots with @Aggregate");
        }
    }

    public Map<Class<?>, List<Method>> getCommandHandlersThatSupportCommand(Command command) {
        Map<Class<?>, List<Method>> result = new java.util.HashMap<>();
        // add default command mapper which maps to the aggregate
        // custom mapper could target a service or a function on how to handle the command
        commandHandlers.forEach(commandHandler -> {
            Class<?> value = commandHandler.getAnnotation(CommandHandler.class)
                .value();
            result.computeIfAbsent(value, k -> new ArrayList<>())
                .add(commandHandler);

        });

        if (MapUtils.isEmpty(result)) {
            log.error("No CommandHandlers found that have a method handle({} command)", command.getClass()
                .getSimpleName());
        }

        return result;
    }

    public Result<List<Object>> onCommand(Command command) {
        Map<Class<?>, List<Method>> aggregateRootToHandlers = getCommandHandlersThatSupportCommand(command);

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
                List<Method> handlers = aggregateRootToHandlers.get(aggregate);
                final AggregateRoot finalAggregateRoot = aggregateRoot;
                handlers.forEach(handler -> {
                    try {
                        Object instance = handler.getDeclaringClass()
                            .getDeclaredConstructor()
                            .newInstance();
                        Optional<Event> result = DewdropReflectionUtils.callMethod(instance, handler.getName(), command);
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
