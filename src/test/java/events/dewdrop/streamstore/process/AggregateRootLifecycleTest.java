package events.dewdrop.streamstore.process;

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

import events.dewdrop.aggregate.AggregateRoot;
import events.dewdrop.api.result.Result;
import events.dewdrop.api.validators.ValidationException;
import events.dewdrop.read.readmodel.stream.StreamFactory;
import events.dewdrop.utils.CommandHandlerUtils;
import events.dewdrop.fixture.automated.DewdropUserAggregate;
import events.dewdrop.fixture.command.DewdropCreateUserCommand;
import events.dewdrop.fixture.events.DewdropUserCreated;
import events.dewdrop.read.readmodel.stream.Stream;
import events.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import events.dewdrop.structure.api.Command;
import events.dewdrop.structure.api.Event;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class AggregateRootLifecycleTest {

    StreamFactory streamFactory;
    AggregateRootLifecycle aggregateRootLifecycle;
    Stream stream;
    AggregateRoot aggregateRoot;
    DewdropUserCreated event;
    Command command;
    Method method;

    @BeforeEach
    void setup() {
        this.streamFactory = mock(StreamFactory.class);
        this.aggregateRootLifecycle = Mockito.spy(new AggregateRootLifecycle(streamFactory));
        this.stream = mock(Stream.class);
        this.aggregateRoot = mock(AggregateRoot.class);
        this.event = mock(DewdropUserCreated.class);
        this.command = new DewdropCreateUserCommand(UUID.randomUUID(), "test");
        this.method = mock(Method.class);
    }

    @Test
    @DisplayName("Given the needed constructor parameters, when the constructor is called, then the object is created")
    void constructor() {
        assertThat(this.aggregateRootLifecycle, is(notNullValue()));
    }

    @Test
    @DisplayName("save() - Given a Stream and an AggregateRoot, when save() is called, then stream.save(aggregateRoot) is called")
    void save() {
        doNothing().when(stream).save(any(AggregateRoot.class));

        aggregateRootLifecycle.save(stream, aggregateRoot);
        verify(stream, times(1)).save(aggregateRoot);
    }

    @Test
    @DisplayName("getById() - Given a Stream, Command, an AggregateRoot and a UUID, when getById() is called, then stream.getById(StreamStoreGetByIDRequest) is called")
    void getById() {
        doReturn(aggregateRoot).when(stream).getById(any(StreamStoreGetByIDRequest.class));
        Command command = mock(DewdropCreateUserCommand.class);

        UUID id = UUID.randomUUID();
        ArgumentCaptor<StreamStoreGetByIDRequest> captor = ArgumentCaptor.forClass(StreamStoreGetByIDRequest.class);
        aggregateRootLifecycle.getById(stream, command, aggregateRoot, id);
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
        aggregateRootLifecycle.processEvents(aggregateRoot, List.of(event, event));
        verify(aggregateRoot, times(2)).raise(any(Event.class));
    }

    @Test
    @DisplayName("processEvents(Event) - Given a single events and an aggregateRoot, when processEvents() is called, then aggregateRoot.raiseEvents(events) is called once")
    void processEvents_once() {
        doNothing().when(aggregateRoot).raise(any(Event.class));
        aggregateRootLifecycle.processEvents(aggregateRoot, event);
        verify(aggregateRoot, times(1)).raise(any(Event.class));
    }

    @Test
    @DisplayName("executeCommand() - Given a command, a method and an aggregateRoot, when executeCommand() is called, then processEvens() is called once")
    void executeCommand() throws ValidationException {
        doNothing().when(aggregateRootLifecycle).processEvents(any(AggregateRoot.class), any(Event.class));
        try (MockedStatic<CommandHandlerUtils> utilities = mockStatic(CommandHandlerUtils.class)) {
            utilities.when(() -> CommandHandlerUtils.executeCommand(any(Method.class), any(Command.class), any(AggregateRoot.class))).thenReturn(Result.of(List.of(event, event)));

            aggregateRootLifecycle.executeCommand(command, method, aggregateRoot);

            verify(aggregateRootLifecycle, times(1)).processEvents(any(AggregateRoot.class), any());
        }
    }


    @Test
    @DisplayName("process() - Given a command, a commandHandlerMethod, an AggregateRoot and a UUID, when process() is called, then create a stream, call getById(), executeCommand() and save()")
    void process() throws ValidationException {
        doReturn(new DewdropUserAggregate()).when(aggregateRoot).getTarget();
        doReturn(aggregateRoot).when(aggregateRootLifecycle).getById(any(Stream.class), any(Command.class), any(AggregateRoot.class), any(UUID.class));
        doReturn(aggregateRoot).when(aggregateRootLifecycle).executeCommand(any(Command.class), any(Method.class), any(AggregateRoot.class));
        doReturn(aggregateRoot).when(aggregateRootLifecycle).save(any(Stream.class), any(AggregateRoot.class));
        doReturn(mock(Stream.class)).when(streamFactory).constructStreamFromAggregateRoot(any(AggregateRoot.class), any(UUID.class));

        Result<Boolean> process = aggregateRootLifecycle.process(command, method, aggregateRoot, UUID.randomUUID());
        assertThat(process.get(), is(notNullValue()));
        verify(streamFactory, times(1)).constructStreamFromAggregateRoot(any(AggregateRoot.class), any(UUID.class));
        verify(aggregateRootLifecycle, times(1)).getById(any(Stream.class), any(Command.class), any(AggregateRoot.class), any(UUID.class));
        verify(aggregateRootLifecycle, times(1)).executeCommand(any(Command.class), any(Method.class), any(AggregateRoot.class));
        verify(aggregateRootLifecycle, times(1)).save(any(Stream.class), any(AggregateRoot.class));
    }
}
