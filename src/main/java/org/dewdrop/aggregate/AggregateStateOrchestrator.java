package org.dewdrop.aggregate;

import java.lang.reflect.Method;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.dewdrop.api.result.Result;
import org.dewdrop.api.validators.ValidationException;
import org.dewdrop.command.CommandMapper;
import org.dewdrop.streamstore.process.AggregateStateCommandProcessor;
import org.dewdrop.structure.api.Command;
import org.dewdrop.structure.events.CorrelationCausation;
import org.dewdrop.utils.AssignCorrelationAndCausation;

@Log4j2
public class AggregateStateOrchestrator {
    private CommandMapper commandMapper;
    private AggregateStateCommandProcessor aggregateStateCommandProcessor;

    public AggregateStateOrchestrator(CommandMapper commandMapper, AggregateStateCommandProcessor aggregateStateCommandProcessor) {
        this.commandMapper = commandMapper;
        this.aggregateStateCommandProcessor = aggregateStateCommandProcessor;
    }

    public <T extends Command> Result<Boolean> executeCommand(T command) throws ValidationException {
        Optional<Method> commandHandlerMethod = commandMapper.getCommandHandlersThatSupportCommand(command);

        if (commandHandlerMethod.isEmpty()) { return Result.empty(); }

        return aggregateStateCommandProcessor.processCommand(command, commandHandlerMethod.get());
    }

    public <T extends Command> Result<Boolean> executeSubsequentCommand(T command, CorrelationCausation previous) throws ValidationException {
        Optional<Method> commandHandlerMethod = commandMapper.getCommandHandlersThatSupportCommand(command);

        if (commandHandlerMethod.isEmpty()) { return Result.empty(); }

        command = AssignCorrelationAndCausation.assignTo(previous, command);
        return aggregateStateCommandProcessor.processCommand(command, commandHandlerMethod.get());
    }

}
