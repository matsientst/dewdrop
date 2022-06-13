package com.dewdrop.streamstore.process;

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

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.api.result.Result;
import com.dewdrop.api.result.ResultException;
import com.dewdrop.fixture.command.DewdropCreateUserCommand;
import com.dewdrop.fixture.events.DewdropUserCreated;
import com.dewdrop.structure.api.Command;
import com.dewdrop.utils.AggregateIdUtils;
import com.dewdrop.utils.AggregateUtils;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class AggregateStateCommandProcessorTest {

    private StreamProcessor streamProcessor;
    private AggregateStateCommandProcessor aggregateStateCommandProcessor;

    @BeforeEach
    void setup() {
        this.streamProcessor = mock(StreamProcessor.class);
        this.aggregateStateCommandProcessor = spy(new AggregateStateCommandProcessor(streamProcessor));
    }

    @Test
    @DisplayName("Given the needed constructor parameters, when the constructor is called, then the object is created")
    void constructor() {
        assertThat(aggregateStateCommandProcessor, is(notNullValue()));
    }

    @Test
    @DisplayName("process() - Given the needed parameters, when process() is called, then streamProcessor.process() is called")
    void process() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "test");
        doReturn(Result.empty()).when(streamProcessor).process(any(), any(Method.class), any(AggregateRoot.class), any(UUID.class));
        Result<DewdropUserCreated> result = aggregateStateCommandProcessor.process(command, mock(Method.class), mock(AggregateRoot.class), UUID.randomUUID());
        assertThat(result, is(notNullValue()));

        verify(streamProcessor, times(1)).process(any(Command.class), any(Method.class), any(AggregateRoot.class), any(UUID.class));
    }

    @Test
    @DisplayName("processCommand() - Given a missingId, when processCommand() is called, then result.empty() is returned")
    void processCommand_noId() {
        AggregateRoot aggregateRoot = mock(AggregateRoot.class);
        try (MockedStatic<AggregateUtils> utilities = mockStatic(AggregateUtils.class)) {
            utilities.when(() -> AggregateUtils.createFromCommandHandlerMethod(any(Method.class))).thenReturn(Optional.of(aggregateRoot));
            try (MockedStatic<AggregateIdUtils> idUtils = mockStatic(AggregateIdUtils.class)) {
                idUtils.when(() -> AggregateIdUtils.getAggregateId(any(Command.class))).thenReturn(Optional.empty());

                Result<DewdropUserCreated> test = aggregateStateCommandProcessor.processCommand(new DewdropCreateUserCommand(UUID.randomUUID(), "test"), mock(Method.class));
                assertThat(test.isEmpty(), is(true));
            }
        }
    }

    @Test
    @DisplayName("processCommand() - Given an invalid commandHandlerMethod, when processCommand() is called, then result.empty() is returned")
    void processCommand_invalidCommandHandlerMethod() {
        try (MockedStatic<AggregateUtils> utilities = mockStatic(AggregateUtils.class)) {
            utilities.when(() -> AggregateUtils.createFromCommandHandlerMethod(any(Method.class))).thenReturn(Optional.empty());
            Result<DewdropUserCreated> test = aggregateStateCommandProcessor.processCommand(new DewdropCreateUserCommand(UUID.randomUUID(), "test"), mock(Method.class));
            assertThat(test.isEmpty(), is(true));
        }
    }

    @Test
    @DisplayName("processCommand() - Given valid parameters, when processCommand() is called, then process() is called")
    void processCommand() throws ResultException {
        AggregateRoot aggregateRoot = mock(AggregateRoot.class);
        doReturn(Result.of(mock(DewdropUserCreated.class))).when(streamProcessor).process(any(), any(Method.class), any(AggregateRoot.class), any(UUID.class));

        try (MockedStatic<AggregateUtils> utilities = mockStatic(AggregateUtils.class)) {
            utilities.when(() -> AggregateUtils.createFromCommandHandlerMethod(any(Method.class))).thenReturn(Optional.of(aggregateRoot));
            try (MockedStatic<AggregateIdUtils> idUtils = mockStatic(AggregateIdUtils.class)) {
                idUtils.when(() -> AggregateIdUtils.getAggregateId(any(Command.class))).thenReturn(Optional.of(UUID.randomUUID()));

                Result<DewdropUserCreated> test = aggregateStateCommandProcessor.processCommand(new DewdropCreateUserCommand(UUID.randomUUID(), "test"), mock(Method.class));
                assertThat(test.get().getClass(), is(DewdropUserCreated.class));

                verify(aggregateStateCommandProcessor, times(1)).process(any(Command.class), any(Method.class), any(AggregateRoot.class), any(UUID.class));
            }
        }
    }
}
