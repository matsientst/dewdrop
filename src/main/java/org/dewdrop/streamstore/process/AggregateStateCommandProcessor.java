package org.dewdrop.streamstore.process;

import static java.util.Objects.requireNonNull;

import org.dewdrop.aggregate.AggregateRoot;
import org.dewdrop.api.result.Result;
import org.dewdrop.api.validators.ValidationException;
import org.dewdrop.structure.api.Command;
import org.dewdrop.utils.AggregateIdUtils;
import org.dewdrop.utils.AggregateUtils;
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

    public Result<Boolean> processCommand(Command command, Method commandHandlerMethod) throws ValidationException {
        Optional<AggregateRoot> optAggregateRoot = AggregateUtils.createFromCommandHandlerMethod(commandHandlerMethod);
        if (optAggregateRoot.isPresent()) {
            AggregateRoot aggregateRoot = optAggregateRoot.get();
            Optional<UUID> optAggregateId = AggregateIdUtils.getAggregateId(command);
            if (optAggregateId.isPresent()) { return process(command, commandHandlerMethod, aggregateRoot, optAggregateId.get()); }
        }
        return Result.of(false);
    }

    Result<Boolean> process(Command command, Method commandHandlerMethod, AggregateRoot aggregateRoot, UUID aggregateRootId) throws ValidationException {
        log.debug("Processing command {}", command.getClass().getSimpleName());
        return streamProcessor.process(command, commandHandlerMethod, aggregateRoot, aggregateRootId);
    }
}
