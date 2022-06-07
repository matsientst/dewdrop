package com.dewdrop.streamstore.process;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.api.result.Result;
import com.dewdrop.api.result.ResultException;
import com.dewdrop.fixture.automated.DewdropUserAggregate;
import com.dewdrop.fixture.command.DewdropCreateUserCommand;
import com.dewdrop.fixture.events.DewdropUserCreated;
import com.dewdrop.read.readmodel.StreamFactory;
import com.dewdrop.read.readmodel.stream.Stream;
import com.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import com.dewdrop.structure.api.Command;
import com.dewdrop.structure.api.Event;
import com.dewdrop.utils.CommandHandlerUtils;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class StreamProcessorTest {

    StreamFactory streamFactory;
    StreamProcessor streamProcessor;
    Stream stream;
    AggregateRoot aggregateRoot;
    DewdropUserCreated event;
    Command command;
    Method method;

    @BeforeEach
    void setup() {
        this.streamFactory = mock(StreamFactory.class);
        this.streamProcessor = spy(new StreamProcessor(streamFactory));
        this.stream = mock(Stream.class);
        this.aggregateRoot = mock(AggregateRoot.class);
        this.event = mock(DewdropUserCreated.class);
        this.command = new DewdropCreateUserCommand(UUID.randomUUID(), "test");
        this.method = mock(Method.class);
    }

    @Test
    @DisplayName("Given the needed constructor parameters, when the constructor is called, then the object is created")
    void constructor() {
        assertThat(this.streamProcessor, is(notNullValue()));
    }

    @Test
    @DisplayName("save() - Given a Stream and an AggregateRoot, when save() is called, then stream.save(aggregateRoot) is called")
    void save() {
        doNothing().when(stream).save(any(AggregateRoot.class));

        streamProcessor.save(stream, aggregateRoot);
        verify(stream, times(1)).save(aggregateRoot);
    }

    @Test
    @DisplayName("getById() - Given a Stream, Command, an AggregateRoot and a UUID, when getById() is called, then stream.getById(StreamStoreGetByIDRequest) is called")
    void getById() {

        doReturn(aggregateRoot).when(stream).getById(any(StreamStoreGetByIDRequest.class));
        Command command = mock(DewdropCreateUserCommand.class);

        UUID id = UUID.randomUUID();
        ArgumentCaptor<StreamStoreGetByIDRequest> captor = ArgumentCaptor.forClass(StreamStoreGetByIDRequest.class);
        streamProcessor.getById(stream, command, aggregateRoot, id);
        verify(stream, times(1)).getById(captor.capture());

        StreamStoreGetByIDRequest request = captor.getValue();
        assertThat(request.getAggregateRoot(), is(aggregateRoot));
        assertThat(request.getCommand(), is(command));
        assertThat(request.getId(), is(id));
    }

    @Test
    @DisplayName("processEvents(List<Event>) - Given a list of events and an aggregateRoot, when processEvents() is called, then aggregateRoot.raiseEvents(events) is called for each event in list")
    void processEvents() {
        doNothing().when(aggregateRoot).raise(any(Event.class));
        streamProcessor.processEvents(aggregateRoot, List.of(event, event));
        verify(aggregateRoot, times(2)).raise(any(Event.class));
    }

    @Test
    @DisplayName("processEvents(Event) - Given a single events and an aggregateRoot, when processEvents() is called, then aggregateRoot.raiseEvents(events) is called once")
    void processEvents_once() {
        doNothing().when(aggregateRoot).raise(any(Event.class));
        streamProcessor.processEvents(aggregateRoot, event);
        verify(aggregateRoot, times(1)).raise(any(Event.class));
    }

    @Test
    @DisplayName("executeCommand() - Given a command, a method and an aggregateRoot, when executeCommand() is called, then processEvens() is called once")
    void executeCommand() {
        doNothing().when(streamProcessor).processEvents(any(AggregateRoot.class), any(Event.class));
        try (MockedStatic<CommandHandlerUtils> utilities = mockStatic(CommandHandlerUtils.class)) {
            utilities.when(() -> CommandHandlerUtils.executeCommand(any(Method.class), any(Command.class), any(AggregateRoot.class))).thenReturn(Optional.of(List.of(event, event)));

            streamProcessor.executeCommand(command, method, aggregateRoot);

            verify(streamProcessor, times(1)).processEvents(any(AggregateRoot.class), any());
        }
    }


    @Test
    @DisplayName("process() - Given a command, a commandHandlerMethod, an AggregateRoot and a UUID, when process() is called, then create a stream, call getById(), executeCommand() and save()")
    void process() throws ResultException {
        doReturn(new DewdropUserAggregate()).when(aggregateRoot).getTarget();
        doReturn(aggregateRoot).when(streamProcessor).getById(any(Stream.class), any(Command.class), any(AggregateRoot.class), any(UUID.class));
        doReturn(aggregateRoot).when(streamProcessor).executeCommand(any(Command.class), any(Method.class), any(AggregateRoot.class));
        doReturn(aggregateRoot).when(streamProcessor).save(any(Stream.class), any(AggregateRoot.class));
        doReturn(mock(Stream.class)).when(streamFactory).constructStream(any(AggregateRoot.class), any(UUID.class));

        Result<DewdropUserAggregate> process = streamProcessor.process(command, method, aggregateRoot, UUID.randomUUID());
        assertThat(process.get(), is(notNullValue()));
        verify(streamFactory, times(1)).constructStream(any(AggregateRoot.class), any(UUID.class));
        verify(streamProcessor, times(1)).getById(any(Stream.class), any(Command.class), any(AggregateRoot.class), any(UUID.class));
        verify(streamProcessor, times(1)).executeCommand(any(Command.class), any(Method.class), any(AggregateRoot.class));
        verify(streamProcessor, times(1)).save(any(Stream.class), any(AggregateRoot.class));
    }
}
