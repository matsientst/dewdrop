package com.dewdrop.command;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.aggregate.proxy.AggregateProxyFactory;
import com.dewdrop.api.result.Result;
import com.dewdrop.streamstore.repository.StreamStoreRepository;
import com.dewdrop.structure.api.Command;
import com.dewdrop.structure.api.Event;
import com.dewdrop.utils.AggregateIdUtils;
import com.dewdrop.utils.AggregateUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DefaultAggregateCommandMapper extends AbstractCommandHandlerMapper {
    public DefaultAggregateCommandMapper() {}

    public void init(StreamStoreRepository streamStoreRepository) {
        super.construct(streamStoreRepository);
        // warming up the aggregateRoot cache
        AggregateUtils.getAnnotatedAggregateRoots();
    }

    public Result<List<Object>> onCommand(Command command) {
        List<Class<?>> aggregateRootsThatSupportCommand = AggregateUtils.getAggregateRootsThatSupportCommand(command);

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
                    Optional<Event> result = aggregateRoot.handleCommand(command);
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
