package org.dewdrop.read.readmodel.stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.dewdrop.aggregate.AggregateRoot;
import org.dewdrop.api.result.Result;
import org.dewdrop.api.result.ResultException;
import org.dewdrop.fixture.automated.DewdropUserAggregate;
import org.dewdrop.fixture.command.DewdropCreateUserCommand;
import org.dewdrop.fixture.events.DewdropUserCreated;
import org.dewdrop.fixture.events.DewdropUserEvent;
import org.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetailsReadModel;
import org.dewdrop.streamstore.eventstore.EventStore;
import org.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import org.dewdrop.streamstore.serialize.JsonSerializer;
import org.dewdrop.streamstore.stream.PrefixStreamNameGenerator;
import org.dewdrop.structure.StreamNameGenerator;
import org.dewdrop.structure.api.Event;
import org.dewdrop.structure.datastore.StreamStore;
import org.dewdrop.structure.events.ReadEventData;
import org.dewdrop.structure.events.StreamReadResults;
import org.dewdrop.structure.read.Direction;
import org.dewdrop.structure.read.ReadRequest;
import org.dewdrop.structure.serialize.EventSerializer;
import org.dewdrop.utils.DependencyInjectionUtils;
import org.dewdrop.utils.DewdropReflectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class StreamReaderTest {

    StreamDetails streamDetails;
    StreamStore streamStore;
    EventSerializer eventSerializer;
    StreamReader streamReader;
    StreamReadResults results;

    @BeforeEach
    void setup() {
        StreamNameGenerator streamNameGenerator = mock(PrefixStreamNameGenerator.class);
        Consumer<Event> eventConsumer = mock(Consumer.class);
        doReturn("DewdropUserAggregate").when(streamNameGenerator).generateForAggregate(any(Class.class), any(UUID.class));
        streamDetails = spy(StreamDetails.builder().streamType(StreamType.AGGREGATE).direction(Direction.FORWARD).aggregateRoot(new AggregateRoot()).streamNameGenerator(streamNameGenerator).id(UUID.randomUUID()).subscribed(true)
                        .messageTypes(List.of(DewdropUserEvent.class)).eventHandler(eventConsumer).create());
        streamStore = mock(EventStore.class);
        eventSerializer = mock(JsonSerializer.class);
        streamReader = spy(StreamReader.getInstance(streamStore, eventSerializer, streamDetails));
        results = spy(new StreamReadResults("Test", 10L, Direction.FORWARD, List.of(mock(ReadEventData.class)), 11L, 10L, true));
    }

    @Test
    @DisplayName("getInstance() - Given a streamStore, eventSerializer, streamDetails, when getInstance() is called, then a valid StreamReader is returned")
    void getInstance() {
        assertThat(streamReader.getStreamStore(), is(streamStore));
        assertThat(streamReader.getEventSerializer(), is(eventSerializer));
        assertThat(streamReader.getStreamDetails(), is(streamDetails));
        assertThat(streamReader.getStreamPosition().get(), is(0L));
        assertThat(streamReader.getNameAndPosition(), is(notNullValue()));
    }

    @Test
    @DisplayName("getInstance() - Given a streamStore, eventSerializer, streamDetails and a default position, when getInstance() is called, then a valid StreamReader is returned with a position that is set")
    void getInstance_defaultPosition() {
        streamReader = StreamReader.getInstance(streamStore, eventSerializer, streamDetails, new AtomicLong(10L));
        assertThat(streamReader.getStreamPosition().get(), is(10L));
    }

    @Test
    @DisplayName("read() - Given a start position, when read() is called, then confirm streamReader.read() is called as well as having a streamPosition set")
    void read() {
        doReturn(results).when(streamStore).read(any(ReadRequest.class));
        doNothing().when(streamReader).eventRead(any(ReadEventData.class));
        streamReader.read(10L, null);

        verify(streamReader, times(1)).eventRead(any(ReadEventData.class));
        assertThat(streamReader.getStreamPosition().get(), is(11L));
    }

    @Test
    @DisplayName("read() - Given a count of 2 and isEndOfStream() is false, when read() is called, then confirm streamReader.eventRead() is called twice")
    void read_multipleLoops() {
        doReturn(false).doReturn(false).doReturn(true).when(results).isEndOfStream();
        doReturn(results).when(streamStore).read(any(ReadRequest.class));
        doNothing().when(streamReader).eventRead(any(ReadEventData.class));
        streamReader.read(10L, 2L);

        verify(streamReader, times(2)).eventRead(any(ReadEventData.class));
        assertThat(streamReader.getStreamPosition().get(), is(11L));
    }

    @Test
    @DisplayName("read() - Given a start position, when stream does not exist, then set streamExists to false and return false")
    void read_streamDoesNotExist() {
        results.setStreamExists(false);
        doReturn(results).when(streamStore).read(any(ReadRequest.class));
        assertThat(streamReader.read(null, 6000L), is(false));

        assertThat(streamReader.isStreamExists(), is(false));
        verify(streamReader, times(0)).eventRead(any(ReadEventData.class));
    }

    @Test
    @DisplayName("read() - Given a start position, when stream does not exist, then set streamExists to false and return false")
    void read_streamDoesNotExist_backward() {
        results.setStreamExists(false);
        streamDetails.setDirection(Direction.BACKWARD);
        doReturn(results).when(streamStore).read(any(ReadRequest.class));
        assertThat(streamReader.read(null, 1L), is(false));

        assertThat(streamReader.isStreamExists(), is(false));
        verify(streamReader, times(0)).eventRead(any(ReadEventData.class));
    }


    @Test
    @DisplayName("eventRead() - Given a ReadEventData, when eventRead() is called, then confirm that the streamDetails.getEventHandler() consumer is called")
    void eventRead() {
        ReadEventData data = mock(ReadEventData.class);
        doReturn(50L).when(data).getEventNumber();
        doReturn(Optional.of(new DewdropUserCreated(UUID.randomUUID(), "Test"))).when(eventSerializer).deserialize(any(ReadEventData.class));

        streamReader.eventRead(data);
        verify(streamDetails, times(1)).getEventHandler();
    }

    @Test
    @DisplayName("eventRead() - Given a ReadEventData, when deserialize() returns an Optional.empty(), then confirm that the streamDetails.getEventHandler() consumer is not called")
    void eventRead_emptyDeserialize() {
        ReadEventData data = mock(ReadEventData.class);
        doReturn(50L).when(data).getEventNumber();
        doReturn(Optional.empty()).when(eventSerializer).deserialize(any(ReadEventData.class));

        streamReader.eventRead(data);
        verify(streamDetails, times(0)).getEventHandler();
    }

    @Test
    @DisplayName("eventRead() - Given a ReadEventData, when eventSerializer.deserialize() throws an exception, then confirm that the streamDetails.getEventHandler() consumer is NOT called")
    void eventRead_unableToDeserialize() {
        ReadEventData data = mock(ReadEventData.class);
        doReturn(50L).when(data).getEventNumber();
        AtomicLong position = mock(AtomicLong.class);
        doReturn(true).when(position).compareAndSet(anyLong(), anyLong());
        doThrow(RuntimeException.class).when(eventSerializer).deserialize(any(ReadEventData.class));
        streamReader.setStreamPosition(position);

        streamReader.eventRead(data);
        verify(streamDetails, times(0)).getEventHandler();
    }

    @Test
    @DisplayName("validateStreamName() - Given a stream name, when calling streamStore.read(), then confirm that streamExists")
    void validateStreamName() {
        doReturn(results).when(streamStore).read(any(ReadRequest.class));
        assertThat(streamReader.validateStreamName("test"), is(true));
    }


    @Test
    @DisplayName("getPosition() - Given firstEvent has been read, when getPosition() is called, then confirm we get the position")
    void getPosition() {
        streamReader.firstEventRead = true;
        long position = 33L;
        streamReader.setStreamPosition(new AtomicLong(position));
        assertThat(streamReader.getPosition(), is(position));
    }

    @Test
    @DisplayName("getPosition() - Given firstEvent has NOT been read, when getPosition() is called, then confirm we get 0L as position")
    void getPosition_notReadYet() {
        streamReader.firstEventRead = false;
        long position = 33L;
        streamReader.setStreamPosition(new AtomicLong(position));
        assertThat(streamReader.getPosition(), is(0L));
    }

    @Test
    @DisplayName("nameAndPosition() - Given a call to nameAndPosition(), when the SubscriptionStartStrategy is READ_ALL_START_END, then confirm readAll() is called")
    void nameAndPosition() {
        doReturn(mock(NameAndPosition.class)).when(streamReader).readAll();
        assertThat(streamReader.nameAndPosition(), is(notNullValue()));

        verify(streamReader, times(1)).readAll();
        verify(streamReader, times(0)).startFromEnd();
    }

    @Test
    @DisplayName("nameAndPosition() - Given a call to nameAndPosition(), when the SubscriptionStartStrategy is START_END_ONLY, then confirm startFromEnd() is called")
    void nameAndPosition_startFromEnd() {
        doReturn(mock(NameAndPosition.class)).when(streamReader).startFromEnd();
        streamReader.getStreamDetails().setSubscriptionStartStrategy(SubscriptionStartStrategy.START_END_ONLY);
        assertThat(streamReader.nameAndPosition(), is(notNullValue()));

        verify(streamReader, times(1)).startFromEnd();
        verify(streamReader, times(0)).readAll();
    }

    @Test
    @DisplayName("nameAndPosition() - Given a call to nameAndPosition(), when the SubscriptionStartStrategy is START_FROM_POSITION, then confirm readFromPosition() is called")
    void nameAndPosition_readFromPosition() {
        doReturn(mock(NameAndPosition.class)).when(streamReader).readFromPosition();
        streamReader.getStreamDetails().setSubscriptionStartStrategy(SubscriptionStartStrategy.START_FROM_POSITION);
        assertThat(streamReader.nameAndPosition(), is(notNullValue()));

        verify(streamReader, times(1)).readFromPosition();
        verify(streamReader, times(0)).readAll();
        verify(streamReader, times(0)).startFromEnd();
    }

    @Test
    @DisplayName("readFromPosition() - Given that the streamDetails has a getStartPositionMethod() and validateStreamName() returns true, when getStartPositionMethod() is called and returns a valid position, then set firstEventRead to true, set the position and return the nameAndPosition")
    void readFromPosition() {
        Method method = mock(Method.class);
        doReturn(DewdropAccountDetailsReadModel.class).when(method).getDeclaringClass();
        doReturn(Optional.of(method)).when(streamDetails).getStartPositionMethod();
        doReturn(true).when(streamReader).validateStreamName(anyString());

        try (MockedStatic<DependencyInjectionUtils> dependencyUtils = mockStatic(DependencyInjectionUtils.class)) {
            dependencyUtils.when(() -> DependencyInjectionUtils.getInstance(any(Class.class))).thenReturn(Optional.of(new DewdropAccountDetailsReadModel()));
            try (MockedStatic<DewdropReflectionUtils> utilities = mockStatic(DewdropReflectionUtils.class)) {
                utilities.when(() -> DewdropReflectionUtils.callMethod(any(), any(Method.class))).thenReturn(Result.of(33L));

                NameAndPosition nameAndPosition = streamReader.readFromPosition();
                assertThat(nameAndPosition.getPosition(), is(33L));

                assertThat(streamReader.getStreamPosition().get(), is(33L));
                assertThat(streamReader.isFirstEventRead(), is(true));
            }
        }
    }

    @Test
    @DisplayName("readFromPosition() - Given that the streamDetails has a getStartPositionMethod() and validateStreamName() returns true, when getStartPositionMethod() is called and returns an invalid position, then return the existing nameAndPosition")
    void readFromPosition_resultException() throws ResultException {
        Method method = mock(Method.class);
        doReturn(DewdropAccountDetailsReadModel.class).when(method).getDeclaringClass();
        doReturn(Optional.of(method)).when(streamDetails).getStartPositionMethod();
        doReturn(true).when(streamReader).validateStreamName(anyString());

        try (MockedStatic<DependencyInjectionUtils> dependencyUtils = mockStatic(DependencyInjectionUtils.class)) {
            dependencyUtils.when(() -> DependencyInjectionUtils.getInstance(any(Class.class))).thenReturn(Optional.of(new DewdropAccountDetailsReadModel()));
            try (MockedStatic<DewdropReflectionUtils> utilities = mockStatic(DewdropReflectionUtils.class)) {
                Result result = mock(Result.class);
                doThrow(new ResultException(new RuntimeException())).when(result).get();
                doReturn(true).when(result).isValuePresent();
                utilities.when(() -> DewdropReflectionUtils.callMethod(any(), any(Method.class))).thenReturn(result);

                assertThrows(RuntimeException.class, () -> streamReader.readFromPosition());
            }
        }
    }

    @Test
    @DisplayName("readFromPosition() - Given that the streamDetails has no startPositionMethod, when readFromPosition() is called, then throw an IllegalStateException")
    void readFromPosition_noMethod() {
        doReturn(Optional.empty()).when(streamDetails).getStartPositionMethod();

        assertThrows(IllegalStateException.class, () -> streamReader.readFromPosition());
    }

    @Test
    @DisplayName("readFromPosition() - Given a valid startPositionMethod, when validateStreamName() returns false, then return the existing nameAndPosition")
    void readFromPosition_invalidStreamName() {
        Method method = mock(Method.class);
        doReturn(Optional.of(method)).when(streamDetails).getStartPositionMethod();
        doReturn(false).when(streamReader).validateStreamName(anyString());

        NameAndPosition nameAndPosition = streamReader.readFromPosition();
        assertThat(nameAndPosition.getPosition(), is(nullValue()));

        assertThat(streamReader.getStreamPosition().get(), is(0L));
        assertThat(streamReader.isFirstEventRead(), is(false));
    }

    @Test
    @DisplayName("readFromPosition() - Given that the streamDetails has a getStartPositionMethod() and validateStreamName() returns true, when getInstance() returns Optional.empty(), then return the existing nameAndPosition")
    void readFromPosition_noInstanceFound() {
        Method method = mock(Method.class);
        doReturn(Optional.of(method)).when(streamDetails).getStartPositionMethod();
        doReturn(DewdropAccountDetailsReadModel.class).when(method).getDeclaringClass();
        doReturn(true).when(streamReader).validateStreamName(anyString());

        try (MockedStatic<DependencyInjectionUtils> dependencyUtils = mockStatic(DependencyInjectionUtils.class)) {
            dependencyUtils.when(() -> DependencyInjectionUtils.getInstance(any(Class.class))).thenReturn(Optional.empty());

            NameAndPosition nameAndPosition = streamReader.readFromPosition();
            assertThat(nameAndPosition.getPosition(), is(nullValue()));

            assertThat(streamReader.getStreamPosition().get(), is(0L));
            assertThat(streamReader.isFirstEventRead(), is(false));
        }
    }

    @Test
    @DisplayName("readFromPosition() - Given that the streamDetails has a getStartPositionMethod() and validateStreamName() returns true, when getInstance() returns Optional.empty(), then return the existing nameAndPosition")
    void readFromPosition_noPosition() {
        Method method = mock(Method.class);
        doReturn(Optional.of(method)).when(streamDetails).getStartPositionMethod();
        doReturn(DewdropAccountDetailsReadModel.class).when(method).getDeclaringClass();
        doReturn(true).when(streamReader).validateStreamName(anyString());

        try (MockedStatic<DependencyInjectionUtils> dependencyUtils = mockStatic(DependencyInjectionUtils.class)) {
            dependencyUtils.when(() -> DependencyInjectionUtils.getInstance(any(Class.class))).thenReturn(Optional.of(new DewdropAccountDetailsReadModel()));
            try (MockedStatic<DewdropReflectionUtils> utilities = mockStatic(DewdropReflectionUtils.class)) {
                utilities.when(() -> DewdropReflectionUtils.callMethod(any(), any(Method.class))).thenReturn(Result.empty());
                NameAndPosition nameAndPosition = streamReader.readFromPosition();
                assertThat(nameAndPosition.getPosition(), is(nullValue()));

                assertThat(streamReader.getStreamPosition().get(), is(0L));
                assertThat(streamReader.isFirstEventRead(), is(false));
            }
        }
    }


    @Test
    @DisplayName("startFromEnd() - Given a call to startFromEnd(), when the streamExists, then confirm we return the streamName and position")
    void startFromEnd() {
        doReturn(results).when(streamStore).read(any(ReadRequest.class));

        NameAndPosition nameAndPosition = streamReader.startFromEnd();
        assertThat(nameAndPosition.getStreamName(), is("DewdropUserAggregate"));
        assertThat(nameAndPosition.getPosition(), is(0L));
    }

    @Test
    @DisplayName("startFromEnd() - Given a call to startFromEnd(), when the stream does not exist, then confirm we return null for streamName and position")
    void startFromEnd_streamDoesNotExist() {
        results.setStreamExists(false);
        doReturn(results).when(streamStore).read(any(ReadRequest.class));

        NameAndPosition nameAndPosition = streamReader.startFromEnd();
        assertThat(nameAndPosition.getStreamName(), is(nullValue()));
        assertThat(nameAndPosition.getPosition(), is(nullValue()));
    }

    @Test
    @DisplayName("readAll() - Given a call to readAll(), when the streamExists, then confirm we return the streamName and position")
    void readAll() {
        doReturn(true).when(streamReader).validateStreamName(anyString());
        doReturn(true).when(streamReader).read(anyLong(), isNull());

        NameAndPosition nameAndPosition = streamReader.readAll();
        assertThat(nameAndPosition.getStreamName(), is("DewdropUserAggregate"));
        assertThat(nameAndPosition.getPosition(), is(0L));

        verify(streamReader, times(1)).read(anyLong(), isNull());
    }

    @Test
    @DisplayName("readAll() - Given a call to readAll(), when the stream does not exist, then confirm we return a null streamName and position")
    void readAll_streamDoesNotExist() {
        doReturn(false).when(streamReader).validateStreamName(anyString());
        doReturn(true).when(streamReader).read(anyLong(), isNull());

        NameAndPosition nameAndPosition = streamReader.readAll();
        assertThat(nameAndPosition.getStreamName(), is(nullValue()));
        assertThat(nameAndPosition.getPosition(), is(nullValue()));

        verify(streamReader, times(0)).read(anyLong(), isNull());
    }

    @Test
    @DisplayName("readAll() - Given a call to readAll(), when read throws a RuntimeException, then confirm we return a null streamName and position")
    void readAll_exception() {
        doThrow(RuntimeException.class).when(streamReader).validateStreamName(anyString());

        NameAndPosition nameAndPosition = streamReader.readAll();
        assertThat(nameAndPosition.getStreamName(), is(nullValue()));
        assertThat(nameAndPosition.getPosition(), is(nullValue()));

        verify(streamReader, times(0)).read(anyLong(), isNull());
    }


    @Test
    @DisplayName("getById() - Given an aggregateRoot, an ID, a version and a command, when we getById(), confirm that the aggregateRoot is returned and that we have deserialized it")
    void getById() {
        AggregateRoot aggregateRoot = new AggregateRoot(new DewdropUserAggregate());
        UUID id = UUID.randomUUID();
        String username = "Test";
        StreamStoreGetByIDRequest request = new StreamStoreGetByIDRequest(aggregateRoot, id, 1, new DewdropCreateUserCommand(id, username));
        doReturn(results).when(streamStore).read(any(ReadRequest.class));
        doReturn(Optional.of(new DewdropUserCreated(id, username))).when(eventSerializer).deserialize(any(ReadEventData.class));
        AggregateRoot byId = streamReader.getById(request);
        assertThat(byId, is(notNullValue()));
        verify(eventSerializer, times(1)).deserialize(any(ReadEventData.class));
    }

    @Test
    @DisplayName("getById() - Given an aggregateRoot, an ID, a version and a command, when we getById(), confirm that the aggregateRoot is returned and that we have deserialized it")
    void getById_notEndOfStream() {
        AggregateRoot aggregateRoot = new AggregateRoot(new DewdropUserAggregate());
        UUID id = UUID.randomUUID();
        String username = "Test";
        StreamStoreGetByIDRequest request = new StreamStoreGetByIDRequest(aggregateRoot, id, 1, new DewdropCreateUserCommand(id, username));
        doReturn(true).doReturn(false).when(streamReader).moreToRead(anyLong(), anyLong(), anyBoolean());
        doReturn(results).when(streamStore).read(any(ReadRequest.class));
        doReturn(Optional.of(new DewdropUserCreated(id, username))).when(eventSerializer).deserialize(any(ReadEventData.class));
        AggregateRoot byId = streamReader.getById(request);
        assertThat(byId, is(notNullValue()));
        verify(eventSerializer, times(2)).deserialize(any(ReadEventData.class));
    }

    @Test
    @DisplayName("getById() - Given an aggregateRoot, an ID, a an invalid version and a command, when we getById(), confirm that the aggregateRoot is returned and that we have deserialized it")
    void getById_invalidVersion() {
        AggregateRoot aggregateRoot = new AggregateRoot(new DewdropUserAggregate());
        UUID id = UUID.randomUUID();
        String username = "Test";
        StreamStoreGetByIDRequest request = new StreamStoreGetByIDRequest(aggregateRoot, id, -100, new DewdropCreateUserCommand(id, username));

        assertThrows(IllegalArgumentException.class, () -> streamReader.getById(request));
    }

    @Test
    @DisplayName("getById() - Given an aggregateRoot, an ID, a version and a null command, when we are unable to deserialize it, confirm that the aggregateRoot is returned and that we have NOT deserialized it")
    void getById_unableToDeserialize() {
        AggregateRoot aggregateRoot = new AggregateRoot(new DewdropUserAggregate());
        UUID id = UUID.randomUUID();
        String username = "Test";
        StreamStoreGetByIDRequest request = new StreamStoreGetByIDRequest(aggregateRoot, id, 1, null);
        doReturn(results).when(streamStore).read(any(ReadRequest.class));
        doReturn(Optional.empty()).when(eventSerializer).deserialize(any(ReadEventData.class));
        AggregateRoot byId = streamReader.getById(request);
        assertThat(byId, is(notNullValue()));
        verify(eventSerializer, times(1)).deserialize(any(ReadEventData.class));
    }

    @Test
    void moreToRead() {
        assertThat(streamReader.moreToRead(0L, 10L, true), is(false));
        assertThat(streamReader.moreToRead(0L, 10L, false), is(false));
        assertThat(streamReader.moreToRead(10L, 0L, true), is(false));
        assertThat(streamReader.moreToRead(10L, 0L, false), is(true));
    }
}
