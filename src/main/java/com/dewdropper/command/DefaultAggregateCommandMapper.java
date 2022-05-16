package com.dewdropper.command;

import com.dewdropper.aggregate.Aggregate;
import com.dewdropper.aggregate.AggregateRoot;
import com.dewdropper.aggregate.proxy.AggregateProxyFactory;
import com.dewdropper.api.result.Result;
import com.dewdropper.streamstore.repository.StreamStoreRepository;
import com.dewdropper.structure.api.Command;
import com.dewdropper.structure.api.Event;
import com.dewdropper.utils.AggregateIdUtils;
import com.dewdropper.utils.ReflectionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

@Log4j2
public class DefaultAggregateCommandMapper implements CommandMapper {
    private StreamStoreRepository streamStoreRepository;

    public DefaultAggregateCommandMapper() {}

    public DefaultAggregateCommandMapper(StreamStoreRepository streamStoreRepository) {
        this.streamStoreRepository = streamStoreRepository;
        init();
    }

    private List<Class<?>> aggregateRoots;

    public void init() {
        aggregateRoots = new ArrayList<>();

        Set<Class<?>> aggregates = ReflectionUtils.getAnnotatedClasses(Aggregate.class);

        aggregates.forEach(aggregate -> {
            aggregateRoots.add(aggregate);
        });

        if (CollectionUtils.isEmpty(aggregateRoots)) {
            log.error("No AggregateRoots found - Make sure to annotate your aggregateRoots with @Aggregate");
        }
    }

    public List<Class<?>> getAggregateRootsThatSupportCommand(Command command) {
        List<Class<?>> result = new ArrayList<>();
        // add default command mapper which maps to the aggregate
        // custom mapper could target a service or a function on how to handle the command
        aggregateRoots.forEach(aggregateRoot -> {
            if (MethodUtils.getMatchingAccessibleMethod(aggregateRoot, "handle", command.getClass()) != null) {
                result.add(aggregateRoot);
            }
        });

        if (CollectionUtils.isEmpty(result)) {
            log.error("No AggregateRoots found that have a method handle({} command)", command.getClass()
                .getSimpleName());
        }

        return result;
    }

    public Result<List<Object>> onCommand(Command command) {
        List<Class<?>> aggregateRootsThatSupportCommand = getAggregateRootsThatSupportCommand(command);

        if (aggregateRootsThatSupportCommand.isEmpty()) {
            return Result.of(new ArrayList<>());
        }

        List<Object> results = new ArrayList<>();
        aggregateRootsThatSupportCommand.forEach(clazz -> {
            Optional<AggregateRoot> optAggregateRoot = AggregateProxyFactory.create(clazz);
            if (optAggregateRoot.isPresent()) {
                AggregateRoot aggregateRoot = optAggregateRoot.get();

                Optional<UUID> aggregateId = AggregateIdUtils.getAggregateId(command);
                if (aggregateId.isPresent()) {
                    aggregateRoot = streamStoreRepository.getById(aggregateId.get(), aggregateRoot, Integer.MAX_VALUE, command)
                        .orElse(aggregateRoot);
                }
                try {
                    Optional<Event> result = ReflectionUtils.callMethod(aggregateRoot.getTarget(), "handle", command);
                    if (result.isPresent()) {
                        aggregateRoot.raise(result.get());
                        streamStoreRepository.save(aggregateRoot);
                        results.add(aggregateRoot.getTarget());
                    }
                } catch (Exception e) {
                    log.error("Error handling command", e);
                }
            }
        });
        return Result.of(results);
    }
}
