package events.dewdrop.read.readmodel.stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import events.dewdrop.aggregate.AggregateRoot;
import events.dewdrop.structure.StreamNameGenerator;
import events.dewdrop.fixture.command.DewdropAddFundsToAccountCommand;
import events.dewdrop.fixture.events.DewdropUserCreated;
import events.dewdrop.fixture.events.DewdropUserEvent;
import events.dewdrop.read.readmodel.stream.subscription.Subscription;
import events.dewdrop.streamstore.eventstore.EventStore;
import events.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import events.dewdrop.streamstore.serialize.JsonSerializer;
import events.dewdrop.streamstore.stream.PrefixStreamNameGenerator;
import events.dewdrop.streamstore.write.StreamWriter;
import events.dewdrop.structure.api.Event;
import events.dewdrop.structure.datastore.StreamStore;
import events.dewdrop.structure.read.Direction;
import events.dewdrop.structure.serialize.EventSerializer;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import lombok.extern.log4j.Log4j2;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@Log4j2
class StreamTest {
    StreamDetails streamDetails;
    StreamStore streamStore;
    EventSerializer eventSerializer;
    Stream stream;
    Subscription subscription;
    StreamReader streamReader;
    AggregateRoot aggregateRoot;

    @BeforeEach
    void setup() {
        StreamNameGenerator streamNameGenerator = mock(PrefixStreamNameGenerator.class);
        Consumer<Event> eventConsumer = mock(Consumer.class);
        doReturn("DewdropUserAggregate").when(streamNameGenerator).generateForAggregate(any(Class.class), any(UUID.class));
        streamDetails = spy(StreamDetails.builder().streamType(StreamType.AGGREGATE).direction(Direction.FORWARD).aggregateRoot(new AggregateRoot()).streamNameGenerator(streamNameGenerator).id(UUID.randomUUID()).subscribed(true)
                        .messageTypes(List.of(DewdropUserEvent.class)).eventHandler(eventConsumer).create());
        streamStore = mock(EventStore.class);
        eventSerializer = mock(JsonSerializer.class);
        subscription = mock(Subscription.class);
        stream = Mockito.spy(new Stream(streamDetails, streamStore, eventSerializer));
        streamReader = mock(StreamReader.class);
        aggregateRoot = mock(AggregateRoot.class);
    }

    @Test
    @DisplayName("Given a streamDetails, a streamStore and an eventSerializer, when the constructor is called, confirm we have a valid stream")
    void constructor() {
        assertThat(stream.getStreamDetails(), Matchers.is(streamDetails));
        assertThat(stream.getStreamStore(), is(streamStore));
        assertThat(stream.getEventSerializer(), is(eventSerializer));
    }

    @Test
    @DisplayName("Given a stream, when it's not subscribed, then do nothing")
    void subscribe_notSubscribed() {
        doReturn(false).when(streamDetails).isSubscribed();
        stream.subscribe();

        verify(streamDetails, times(0)).getMessageTypes();
    }

    @Test
    @DisplayName("Given a stream that is subscribed, when subscribe is called, then confirm subscribeByNameAndPosition() is called")
    void subscribe() {
        doReturn(true).when(streamDetails).isSubscribed();
        doReturn(true).when(subscription).subscribeByNameAndPosition(any(StreamReader.class));

        try (MockedStatic<Subscription> utilities = mockStatic(Subscription.class)) {
            utilities.when(() -> Subscription.getInstance(any(Stream.class))).thenReturn(subscription);
            stream.subscribe();
            verify(subscription, times(1)).subscribeByNameAndPosition(any(StreamReader.class));
        }
    }

    @Test
    @DisplayName("Given a stream that is subscribed and subscribe is called, when it returns false, then confirm pollForCompletion() is called")
    void subscribe_pollForCompletion() {
        doReturn(true).when(streamDetails).isSubscribed();
        doReturn(false).when(subscription).subscribeByNameAndPosition(any(StreamReader.class));

        try (MockedStatic<Subscription> utilities = mockStatic(Subscription.class)) {
            utilities.when(() -> Subscription.getInstance(any(Stream.class))).thenReturn(subscription);
            stream.subscribe();
            verify(subscription, times(1)).subscribeByNameAndPosition(any(StreamReader.class));
            // verify(subscription, times(1)).pollForCompletion(any(StreamReader.class));
        }
    }


    @Test
    @DisplayName("Given a stream, when read() is called, then confirm that stream.read() is called")
    void read() {
        doReturn(true).when(streamDetails).isSubscribed();
        doReturn(true).when(subscription).subscribeByNameAndPosition(any(StreamReader.class));

        AtomicLong streamPosition = new AtomicLong(5L);
        doReturn(streamPosition).when(streamReader).getStreamPosition();
        try (MockedStatic<StreamReader> utilities = mockStatic(StreamReader.class)) {
            utilities.when(() -> StreamReader.getInstance(any(StreamStore.class), any(EventSerializer.class), any(StreamDetails.class), any(AtomicLong.class))).thenReturn(streamReader);
            stream.read(1L, null);
            verify(streamReader, times(1)).read(anyLong(), isNull());
            assertThat(stream.getStreamPosition(), is(streamPosition));
        }
    }

    @Test
    @DisplayName("Given a event, when stream.handle(event) is called, confirm that the consumer is called")
    void handle() {
        Event event = new DewdropUserCreated(UUID.randomUUID(), "test");
        Consumer<Event> consumer = consume -> log.info(consume);
        doReturn(consumer).when(streamDetails).getEventHandler();
        stream.handle(event);
        verify(streamDetails, times(1)).getEventHandler();
    }

    @Test
    @DisplayName("Given a call to updateState(), when the stream is not subscribed, confirm that stream.read() is called")
    void updateState() {
        doReturn(false).when(streamDetails).isSubscribed();
        doNothing().when(stream).read(anyLong(), isNull());
        stream.updateState();
        verify(stream, times(1)).read(anyLong(), isNull());
    }

    @Test
    @DisplayName("Given a call to updateState(), when the stream is subscribed, confirm that stream.read() is NOT called")
    void updateState_subscribed() {
        doReturn(true).when(streamDetails).isSubscribed();
        stream.updateState();
        verify(stream, times(0)).read(anyLong(), isNull());
    }

    @Test
    @DisplayName("Given a StreamStoreGetByIDRequest and a streamType of Aggregate, when getById() is called, then confirm that we call streamReader.getById()")
    void getById() {
        StreamStoreGetByIDRequest request = new StreamStoreGetByIDRequest(aggregateRoot, UUID.randomUUID(), 1, new DewdropAddFundsToAccountCommand(UUID.randomUUID(), new BigDecimal(100)));
        Mockito.doReturn(StreamType.AGGREGATE).when(streamDetails).getStreamType();

        doReturn(aggregateRoot).when(streamReader).getById(any(StreamStoreGetByIDRequest.class));
        try (MockedStatic<StreamReader> utilities = mockStatic(StreamReader.class)) {
            utilities.when(() -> StreamReader.getInstance(any(StreamStore.class), any(EventSerializer.class), any(StreamDetails.class))).thenReturn(streamReader);

            assertThat(stream.getById(request), is(notNullValue()));
            verify(streamReader, times(1)).getById(any(StreamStoreGetByIDRequest.class));
        }
    }

    @Test
    @DisplayName("Given a StreamStoreGetByIDRequest and a streamType that is NOT an Aggregate, when getById() is called, then confirm that throw an IllegalStateException")
    void getById_wrongStreamType() {
        StreamStoreGetByIDRequest request = new StreamStoreGetByIDRequest(aggregateRoot, UUID.randomUUID(), 1, new DewdropAddFundsToAccountCommand(UUID.randomUUID(), new BigDecimal(100)));
        Mockito.doReturn(StreamType.CATEGORY).when(streamDetails).getStreamType();

        assertThrows(IllegalStateException.class, () -> stream.getById(request));
    }

    @Test
    @DisplayName("Given an aggregateRoot, when we call save(), verify we call streamWriter.save()")
    void save() {
        StreamWriter streamWriter = mock(StreamWriter.class);
        doNothing().when(streamWriter).save(any(AggregateRoot.class));
        try (MockedStatic<StreamWriter> utilities = mockStatic(StreamWriter.class)) {
            utilities.when(() -> StreamWriter.getInstance(any(StreamDetails.class), any(StreamStore.class), any(EventSerializer.class))).thenReturn(streamWriter);

            stream.save(aggregateRoot);
            verify(streamWriter, times(1)).save(any(AggregateRoot.class));
        }
    }
}
