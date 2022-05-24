package com.dewdrop.aggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dewdrop.api.result.Result;
import com.dewdrop.api.result.ResultException;
import com.dewdrop.command.CommandHandlerMapper;
import com.dewdrop.command.CommandMapper;
import com.dewdrop.fixture.DewdropAccountAggregate;
import com.dewdrop.fixture.DewdropAccountCreated;
import com.dewdrop.fixture.DewdropAddFundsToAccountCommand;
import com.dewdrop.fixture.DewdropCreateAccountCommand;
import com.dewdrop.read.readmodel.StreamDetailsFactory;
import com.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import com.dewdrop.streamstore.repository.StreamStoreRepository;
import com.dewdrop.structure.StreamNameGenerator;
import com.dewdrop.structure.api.Command;
import com.dewdrop.utils.AggregateIdUtils;
import com.dewdrop.utils.CommandUtils;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@Log4j2
class AggregateStateOrchestratorTest {

    AggregateStateOrchestrator aggregateStateOrchestrator;
    CommandMapper commandMapper;
    DewdropAccountAggregate dewdropAccountAggregate;
    DewdropCreateAccountCommand dewdropCreateAccountCommand;
    Method handleMethod;
    StreamDetailsFactory streamDetailsFactory;
    StreamStoreRepository streamStoreRepository;
    String name;
    UUID id;
    UUID userId;

    @BeforeEach
    void setup() throws NoSuchMethodException {
        id = UUID.randomUUID();
        userId = UUID.randomUUID();
        name = "TestName";
        dewdropCreateAccountCommand = new DewdropCreateAccountCommand(id, name, userId);
        streamDetailsFactory = new StreamDetailsFactory(mock(StreamNameGenerator.class));
        streamStoreRepository = mock(StreamStoreRepository.class);
        commandMapper = spy(new CommandHandlerMapper());
        aggregateStateOrchestrator = spy(new AggregateStateOrchestrator(commandMapper, streamStoreRepository, streamDetailsFactory));
        dewdropAccountAggregate = spy(new DewdropAccountAggregate());
        handleMethod = DewdropAccountAggregate.class.getMethod("handle", DewdropCreateAccountCommand.class);

        doReturn(dewdropAccountAggregate).when(streamStoreRepository)
            .getById(any(StreamStoreGetByIDRequest.class));
        doNothing().when(streamStoreRepository)
            .save(any(AggregateRoot.class));
    }

    @Test
    @DisplayName("Given that no valid commandHandlerMethods exist on the aggregate, an empty ArrayList is returned.")
    void executeCommand_NoCommandHandlerMethod() throws ResultException {
        Result<Object> result = aggregateStateOrchestrator.executeCommand(mock(Command.class));

        assertThat(((ArrayList<?>) result.get()).isEmpty(), is(true));
    }

    @Test
    @DisplayName("Given a properly annotated DewDropAccountAggregate class and a valid command, the command is processed and the Aggregate's state is updated.")
    void executeCommand() throws ResultException {
        doReturn(Optional.of(handleMethod)).when(commandMapper)
            .getCommandHandlersThatSupportCommand(any(Command.class));

        Result<Object> result = aggregateStateOrchestrator.executeCommand(dewdropCreateAccountCommand);

        verify(aggregateStateOrchestrator, times(1)).processCommand(any(Command.class), any(Method.class));
        assertThat(((DewdropAccountAggregate) result.get()).getAccountId(), is(id));
        assertThat(((DewdropAccountAggregate) result.get()).getName(), is(name));
        assertThat(((DewdropAccountAggregate) result.get()).getBalance(), is(new BigDecimal(0)));
    }

    @Test
    @DisplayName("Given that no valid commandHandlerMethods exist on the aggregate, an empty ArrayList is returned.")
    void executeSubsequentCommand_NoCommandHandlerMethod() throws ResultException {
        Result<Object> result = aggregateStateOrchestrator.executeSubsequentCommand(mock(Command.class), mock(Command.class));

        assertThat(((ArrayList<?>) result.get()).isEmpty(), is(true));
    }

    @Test
    @DisplayName("Given a previous command has been executed successfully, a related subsequent command is executed. The aggregate is updated with the new command, and the correlation Id and causation Id are set on the messages.")
    void executeSubsequentCommand() throws ResultException {

        BigDecimal depositedAmount = new BigDecimal(100);

        DewdropCreateAccountCommand previous = new DewdropCreateAccountCommand(id, name, userId);
        DewdropAddFundsToAccountCommand command = new DewdropAddFundsToAccountCommand(id, depositedAmount);

        doReturn(Optional.of(handleMethod)).when(commandMapper)
            .getCommandHandlersThatSupportCommand(any(Command.class));

        Result<Object> firstResult = aggregateStateOrchestrator.executeCommand(previous);

        // Verify the initial command was executed correctly.
        assertThat(((DewdropAccountAggregate) firstResult.get()).getAccountId(), is(id));
        assertThat(((DewdropAccountAggregate) firstResult.get()).getBalance(), is(new BigDecimal(0)));

        Result<Object> finalResult = aggregateStateOrchestrator.executeSubsequentCommand(command, previous);

        // Verify the subsequent command was executed correctly.
        assertThat(((DewdropAccountAggregate) finalResult.get()).getAccountId(), is(id));
        assertThat(((DewdropAccountAggregate) finalResult.get()).getBalance(), is(new BigDecimal(100)));

        // Verify the causation and correlation Ids have been set on the messages.
        assertThat(command.getCausationId(), is(previous.getMessageId()));
        assertThat(command.getCorrelationId(), is(previous.getCorrelationId()));

    }

    @Test
    @DisplayName("Given a method of an Aggregate that is not annotated with the CommandHandler annotation is processed, an empty Result is returned.")
    void processCommand_NoAggregateRootFound() throws NoSuchMethodException {
        Result<Object> result = aggregateStateOrchestrator.processCommand(mock(Command.class), DewdropAccountAggregate.class.getMethod("on", DewdropAccountCreated.class));

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    @DisplayName("Given a properly annotated DewDropAccountAggregate class handle method and a valid command, a Result containing the Aggregate's updated state is returned.")
    void processCommand() throws ResultException {
        Result<Object> result = aggregateStateOrchestrator.processCommand(dewdropCreateAccountCommand, handleMethod);

        assertThat(((DewdropAccountAggregate) result.get()).getAccountId(), is(id));
        assertThat(((DewdropAccountAggregate) result.get()).getName(), is(name));
    }

    @Test
    @DisplayName("Given a valid AggregateRoot, save is called on the StreamStoreRepository and the AggregateRoot is returned.")
    void save() {
        AggregateRoot result = aggregateStateOrchestrator.save(dewdropAccountAggregate);

        verify(streamStoreRepository, times(1)).save(any(result.getClass()));
        assertThat(result, is(dewdropAccountAggregate));
    }

    @Test
    @DisplayName("Given no events are returned when executing a command, the unmodified AggregateRoot is returned.")
    void executeCommandOverloaded_EmptyEvents() {
        AggregateRoot unmodifiedAggregateRoot = dewdropAccountAggregate;

        try (MockedStatic<CommandUtils> utils = mockStatic(CommandUtils.class)) {
            utils.when(() -> CommandUtils.executeCommand(any(), any(Method.class), any(Command.class), any(AggregateRoot.class)))
                .thenReturn(Optional.empty());

            AggregateRoot result = aggregateStateOrchestrator.executeCommand(dewdropCreateAccountCommand, handleMethod, dewdropAccountAggregate);

            assertThat(result, is(unmodifiedAggregateRoot));
            assertThat(((DewdropAccountAggregate) result).getBalance(), is(new BigDecimal(0)));
            assertNull(((DewdropAccountAggregate) result).getAccountId());
            assertNull(((DewdropAccountAggregate) result).getName());
        }
    }

    @Test
    @DisplayName("Given a valid command, handler method, and AggregateRoot, the state of the AggregateRoot target is updated and the whole AggregateRoot is returned.")
    void executeCommandOverloaded() {
        DewdropAccountAggregate result = (DewdropAccountAggregate) aggregateStateOrchestrator.executeCommand(dewdropCreateAccountCommand, handleMethod, dewdropAccountAggregate)
            .getTarget();

        assertThat(result.getAccountId(), is(id));
        assertThat(result.getName(), is(name));
        assertThat(result.getBalance(), is(new BigDecimal(0)));
    }

    @Test
    @DisplayName("Given AggregateIdUtils.getAggregateId cannot find an AggregateRoot, the StreamStore is not called and the unmodified AggregateRoot is returned.")
    void getById_OptionalEmpty() {
        AggregateRoot unmodifiedAggregateRoot = dewdropAccountAggregate;

        try (MockedStatic<AggregateIdUtils> utils = mockStatic(AggregateIdUtils.class)) {
            utils.when(() -> AggregateIdUtils.getAggregateId(any(Command.class)))
                .thenReturn(Optional.empty());

            AggregateRoot result = aggregateStateOrchestrator.getById(dewdropCreateAccountCommand, dewdropAccountAggregate);

            verify(streamStoreRepository, times(0)).getById(any(StreamStoreGetByIDRequest.class));
            assertThat(result, is(unmodifiedAggregateRoot));
            assertThat(((DewdropAccountAggregate) result).getBalance(), is(new BigDecimal(0)));
            assertNull(((DewdropAccountAggregate) result).getAccountId());
            assertNull(((DewdropAccountAggregate) result).getName());
        }
    }

    @Test
    @DisplayName("Given an aggregateId is found by AggregateIdUtils.getAggregateId, it returns a result from the StreamStoreRepository")
    void getById() {
        AggregateRoot modifiedAggregateRoot = new AggregateRoot();
        long version = 512L;
        modifiedAggregateRoot.setVersion(version);

        try (MockedStatic<AggregateIdUtils> utils = mockStatic(AggregateIdUtils.class)) {
            utils.when(() -> AggregateIdUtils.getAggregateId(any(Command.class)))
                .thenReturn(Optional.of(UUID.randomUUID()));

            doReturn(modifiedAggregateRoot).when(streamStoreRepository)
                .getById(any(StreamStoreGetByIDRequest.class));

            AggregateRoot result = aggregateStateOrchestrator.getById(dewdropCreateAccountCommand, dewdropAccountAggregate);

            verify(streamStoreRepository, times(1)).getById(any(StreamStoreGetByIDRequest.class));
            assertThat(result.getVersion(), is(version));
        }
    }

}