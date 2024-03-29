package events.dewdrop.aggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import events.dewdrop.api.result.Result;
import events.dewdrop.api.validators.ValidationException;
import events.dewdrop.command.CommandHandlerMapper;
import events.dewdrop.command.CommandMapper;
import events.dewdrop.fixture.automated.DewdropAccountAggregate;
import events.dewdrop.fixture.command.DewdropCreateAccountCommand;
import events.dewdrop.streamstore.process.AggregateStateCommandProcessor;
import events.dewdrop.structure.api.Command;
import events.dewdrop.utils.AssignCorrelationAndCausation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@Log4j2
class AggregateStateOrchestratorTest {

    AggregateStateOrchestrator aggregateStateOrchestrator;
    CommandMapper commandMapper;
    DewdropAccountAggregate dewdropAccountAggregate;
    DewdropCreateAccountCommand dewdropCreateAccountCommand;
    AggregateStateCommandProcessor aggregateStateCommandProcessor;
    Method handleMethod;

    @BeforeEach
    void setup() {
        dewdropCreateAccountCommand = mock(DewdropCreateAccountCommand.class);
        commandMapper = mock(CommandHandlerMapper.class);
        aggregateStateCommandProcessor = mock(AggregateStateCommandProcessor.class);
        aggregateStateOrchestrator = Mockito.spy(new AggregateStateOrchestrator(commandMapper, aggregateStateCommandProcessor));
        dewdropAccountAggregate = spy(new DewdropAccountAggregate());
        handleMethod = mock(Method.class);
    }

    @Test
    @DisplayName("Given that no valid commandHandlerMethods exist on the aggregate, an empty ArrayList is returned.")
    void executeCommand_NoCommandHandlerMethod() throws ValidationException {
        doReturn(Optional.empty()).when(commandMapper).getCommandHandlersThatSupportCommand(any(Command.class));
        Result<Boolean> result = aggregateStateOrchestrator.executeCommand(mock(Command.class));

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    @DisplayName("Given a properly annotated DewDropAccountAggregate class and a valid command, the command is processed and the Aggregate's state is updated.")
    void executeCommand() throws ValidationException {
        doReturn(Result.of(true)).when(aggregateStateCommandProcessor).processCommand(any(Command.class), any(Method.class));
        doReturn(Optional.of(handleMethod)).when(commandMapper).getCommandHandlersThatSupportCommand(any(DewdropCreateAccountCommand.class));

        Result<Boolean> results = aggregateStateOrchestrator.executeCommand(dewdropCreateAccountCommand);
        assertThat(results.get(), is(true));

        verify(aggregateStateCommandProcessor, times(1)).processCommand(any(Command.class), any(Method.class));
    }

    @Test
    @DisplayName("Given that no valid commandHandlerMethods exist on the aggregate, an empty ArrayList is returned.")
    void executeSubsequentCommand_NoCommandHandlerMethod() throws ValidationException {
        doReturn(Optional.empty()).when(commandMapper).getCommandHandlersThatSupportCommand(any(Command.class));
        Result<Boolean> result = aggregateStateOrchestrator.executeSubsequentCommand(mock(Command.class), mock(Command.class));

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    @DisplayName("Given a previous command has been executed successfully, a related subsequent command is executed. The aggregate is updated with the new command, and the correlation Id and causation Id are set on the messages.")
    void executeSubsequentCommand() throws ValidationException {
        doReturn(Optional.of(handleMethod)).when(commandMapper).getCommandHandlersThatSupportCommand(any(Command.class));
        doReturn(Result.of(dewdropAccountAggregate)).when(aggregateStateCommandProcessor).processCommand(any(Command.class), any(Method.class));

        DewdropCreateAccountCommand previous = mock(DewdropCreateAccountCommand.class);
        DewdropCreateAccountCommand subsequent = mock(DewdropCreateAccountCommand.class);
        try (MockedStatic<AssignCorrelationAndCausation> utilities = mockStatic(AssignCorrelationAndCausation.class)) {
            utilities.when(() -> AssignCorrelationAndCausation.assignTo(any(Command.class), any(Command.class))).thenReturn(previous);
            Result<Boolean> result = aggregateStateOrchestrator.executeSubsequentCommand(previous, subsequent);

            assertThat(result.isEmpty(), is(false));
        }
    }
}
