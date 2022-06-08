package com.dewdrop.aggregate;

import com.dewdrop.api.result.Result;
import com.dewdrop.command.CommandMapper;
import com.dewdrop.streamstore.process.AggregateStateCommandProcessor;
import com.dewdrop.structure.api.Command;
import com.dewdrop.structure.events.CorrelationCausation;
import com.dewdrop.utils.AssignCorrelationAndCausation;
import java.lang.reflect.Method;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AggregateStateOrchestrator {
    private CommandMapper commandMapper;
    private AggregateStateCommandProcessor aggregateStateCommandProcessor;

    public AggregateStateOrchestrator(CommandMapper commandMapper, AggregateStateCommandProcessor aggregateStateCommandProcessor) {
        this.commandMapper = commandMapper;
        this.aggregateStateCommandProcessor = aggregateStateCommandProcessor;
    }

    public <T> Result<T> executeCommand(Command command) {
        Optional<Method> commandHandlerMethod = commandMapper.getCommandHandlersThatSupportCommand(command);

        if (commandHandlerMethod.isEmpty()) { return Result.empty(); }

        return aggregateStateCommandProcessor.processCommand(command, commandHandlerMethod.get());
    }

    public <T> Result<T> executeSubsequentCommand(Command command, CorrelationCausation previous) {
        Optional<Method> commandHandlerMethod = commandMapper.getCommandHandlersThatSupportCommand(command);

        if (commandHandlerMethod.isEmpty()) { return Result.empty(); }

        command = AssignCorrelationAndCausation.assignTo(previous, command);
        return aggregateStateCommandProcessor.processCommand(command, commandHandlerMethod.get());
    }

}
