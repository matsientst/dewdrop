package com.dewdrop.command;

import com.dewdrop.aggregate.Aggregate;
import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.aggregate.proxy.AggregateProxyFactory;
import com.dewdrop.api.result.Result;
import com.dewdrop.streamstore.repository.StreamStoreRepository;
import com.dewdrop.structure.api.Command;
import com.dewdrop.structure.api.Event;
import com.dewdrop.utils.AggregateIdUtils;
import com.dewdrop.utils.AnnotationReflection;
import com.dewdrop.utils.ReflectionUtils;
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

        Set<Class<?>> aggregates = AnnotationReflection.getAnnotatedClasses(Aggregate.class);

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
                    UUID id = aggregateId.get();
                    aggregateRoot = streamStoreRepository.getById(id, aggregateRoot, Integer.MAX_VALUE, command)
                        .orElse(aggregateRoot);
                    if(aggregateRoot.getCausationId() == null) {
                        aggregateRoot.setCausationId(id);
                        aggregateRoot.setCorrelationId(id);
                    }
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
