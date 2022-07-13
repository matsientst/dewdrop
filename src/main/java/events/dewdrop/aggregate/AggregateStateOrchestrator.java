package events.dewdrop.aggregate;

import events.dewdrop.api.result.Result;
import events.dewdrop.api.validators.ValidationException;
import events.dewdrop.command.CommandMapper;
import events.dewdrop.streamstore.process.AggregateStateCommandProcessor;
import events.dewdrop.utils.AssignCorrelationAndCausation;
import java.lang.reflect.Method;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import events.dewdrop.structure.api.Command;
import events.dewdrop.structure.events.CorrelationCausation;

@Log4j2
/**
 * This class is responsible for orchestrating the stateful processing of events for a single
 * aggregate
 */
public class AggregateStateOrchestrator {
    private CommandMapper commandMapper;
    private AggregateStateCommandProcessor aggregateStateCommandProcessor;

    public AggregateStateOrchestrator(CommandMapper commandMapper, AggregateStateCommandProcessor aggregateStateCommandProcessor) {
        this.commandMapper = commandMapper;
        this.aggregateStateCommandProcessor = aggregateStateCommandProcessor;
    }

    /**
     * Retrieves the appropriate command handler method for the given command and invokes it on the
     * AggregateRoot
     *
     * @param command The command to execute.
     * @return Result<Boolean>
     */
    public <T extends Command> Result<Boolean> executeCommand(T command) throws ValidationException {
        Optional<Method> commandHandlerMethod = commandMapper.getCommandHandlersThatSupportCommand(command);

        if (commandHandlerMethod.isEmpty()) { return Result.empty(); }

        return aggregateStateCommandProcessor.processCommand(command, commandHandlerMethod.get());
    }

    /**
     * Retrieves the appropriate command handler method for the given command and invokes it on the
     * AggregateRoot If the command has a previous, related command that you want to keep the causation
     * and correlationIds for then this assigns the correlation and causation to the command and process
     * it.
     *
     * @param command The command to execute
     * @param previous The previous command that was executed.
     * @return A Result<Boolean>
     */
    public <T extends Command> Result<Boolean> executeSubsequentCommand(T command, CorrelationCausation previous) throws ValidationException {
        Optional<Method> commandHandlerMethod = commandMapper.getCommandHandlersThatSupportCommand(command);

        if (commandHandlerMethod.isEmpty()) { return Result.empty(); }

        command = AssignCorrelationAndCausation.assignTo(previous, command);
        return aggregateStateCommandProcessor.processCommand(command, commandHandlerMethod.get());
    }

}
