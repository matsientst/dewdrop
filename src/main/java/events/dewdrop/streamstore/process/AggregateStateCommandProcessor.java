package events.dewdrop.streamstore.process;

import static java.util.Objects.requireNonNull;

import events.dewdrop.aggregate.AggregateRoot;
import events.dewdrop.api.result.Result;
import events.dewdrop.api.validators.ValidationException;
import events.dewdrop.utils.AggregateUtils;
import events.dewdrop.structure.api.Command;
import events.dewdrop.utils.AggregateIdUtils;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

/**
 * This class is responsible for processing commands that mutate the state of an aggregate
 */
@Log4j2
public class AggregateStateCommandProcessor {
    AggregateRootLifecycle streamProcessor;

    private AggregateStateCommandProcessor() {}

    public AggregateStateCommandProcessor(AggregateRootLifecycle aggregateRootLifecycle) {
        requireNonNull(aggregateRootLifecycle, "StreamProcessor is required");

        this.streamProcessor = aggregateRootLifecycle;
    }

    /**
     * This method constructs the AggregateRoot and processes the command by invoking the AggregateRoot
     * lifecycle.
     *
     * @param command The command to be processed.
     * @param commandHandlerMethod The method that will be invoked to process the command.
     * @return A Result<Boolean>
     */
    public Result<Boolean> processCommand(Command command, Method commandHandlerMethod) throws ValidationException {
        Optional<AggregateRoot> optAggregateRoot = AggregateUtils.createFromCommandHandlerMethod(commandHandlerMethod);
        if (optAggregateRoot.isPresent()) {
            AggregateRoot aggregateRoot = optAggregateRoot.get();
            Optional<UUID> optAggregateId = AggregateIdUtils.getAggregateId(command);
            if (optAggregateId.isPresent()) { return process(command, commandHandlerMethod, aggregateRoot, optAggregateId.get()); }
        }
        return Result.of(false);
    }

    /**
     * It takes a command, a command handler method, an aggregate root and an aggregate root id. Then it
     * invokes the AggregateRoot lifecycle and returns a result of a boolean
     *
     * @param command The command to be processed
     * @param commandHandlerMethod The method that will be invoked to process the command.
     * @param aggregateRoot The aggregate root that the command is being applied to.
     * @param aggregateRootId The id of the aggregate root that the command is being sent to.
     * @return A CommandResult<Boolean>
     */
    Result<Boolean> process(Command command, Method commandHandlerMethod, AggregateRoot aggregateRoot, UUID aggregateRootId) throws ValidationException {
        log.debug("Processing command {}", command.getClass().getSimpleName());
        return streamProcessor.process(command, commandHandlerMethod, aggregateRoot, aggregateRootId);
    }
}
