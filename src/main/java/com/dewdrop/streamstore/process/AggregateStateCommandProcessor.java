package com.dewdrop.streamstore.process;

import static java.util.Objects.requireNonNull;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.api.result.Result;
import com.dewdrop.structure.api.Command;
import com.dewdrop.utils.AggregateIdUtils;
import com.dewdrop.utils.AggregateUtils;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AggregateStateCommandProcessor {
    StreamProcessor streamProcessor;

    private AggregateStateCommandProcessor() {}

    public AggregateStateCommandProcessor(StreamProcessor streamProcessor) {
        requireNonNull(streamProcessor, "StreamProcessor is required");

        this.streamProcessor = streamProcessor;
    }

    public <T> Result<T> processCommand(Command command, Method commandHandlerMethod) {
        Optional<AggregateRoot> optAggregateRoot = AggregateUtils.createFromCommandHandlerMethod(commandHandlerMethod);
        if (optAggregateRoot.isPresent()) {
            AggregateRoot aggregateRoot = optAggregateRoot.get();
            Optional<UUID> optAggregateId = AggregateIdUtils.getAggregateId(command);
            if (optAggregateId.isPresent()) { return process(command, commandHandlerMethod, aggregateRoot, optAggregateId.get()); }
        }
        return Result.empty();
    }

    <T> Result<T> process(Command command, Method commandHandlerMethod, AggregateRoot aggregateRoot, UUID aggregateRootId) {
        log.debug("Processing command {}", command.getClass().getSimpleName());
        return streamProcessor.process(command, commandHandlerMethod, aggregateRoot, aggregateRootId);
    }
}
