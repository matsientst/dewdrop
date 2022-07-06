package org.dewdrop.read.readmodel.stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.dewdrop.aggregate.AggregateRoot;
import org.dewdrop.fixture.automated.DewdropAccountAggregate;
import org.dewdrop.fixture.events.DewdropAccountCreated;
import org.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetailsReadModel;
import org.dewdrop.read.readmodel.ReadModel;
import org.dewdrop.read.readmodel.ReadModelWrapper;
import org.dewdrop.read.readmodel.annotation.Stream;
import org.dewdrop.read.readmodel.cache.InMemoryCacheProcessor;
import org.dewdrop.read.readmodel.cache.MapBackedInMemoryCacheProcessor;
import org.dewdrop.streamstore.stream.PrefixStreamNameGenerator;
import org.dewdrop.structure.StreamNameGenerator;
import org.dewdrop.structure.datastore.StreamStore;
import org.dewdrop.structure.read.Direction;
import org.dewdrop.structure.serialize.EventSerializer;
import org.dewdrop.utils.EventHandlerUtils;
import org.dewdrop.utils.StreamUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class StreamFactoryTest {
    StreamFactory streamFactory;
    StreamStore streamStore;
    EventSerializer eventSerializer;
    StreamNameGenerator streamNameGenerator;
    org.dewdrop.read.readmodel.annotation.Stream streamAnnotation;
    InMemoryCacheProcessor inMemoryCacheProcessor;
    ReadModel readModel;
    StreamType streamType;
    Direction direction;
    boolean subscribed;
    String name;

    @BeforeEach
    void setup() {
        streamStore = mock(StreamStore.class);
        eventSerializer = mock(EventSerializer.class);
        streamNameGenerator = new PrefixStreamNameGenerator();
        streamFactory = spy(new StreamFactory(streamStore, eventSerializer, streamNameGenerator));

        streamAnnotation = spy(TestReadModel.class.getAnnotation(Stream.class));
        inMemoryCacheProcessor = mock(MapBackedInMemoryCacheProcessor.class);
        readModel = new ReadModel(ReadModelWrapper.of(DewdropAccountDetailsReadModel.class).get(), Optional.of(inMemoryCacheProcessor));
        streamType = StreamType.CATEGORY;
        direction = Direction.FORWARD;
        subscribed = true;
        name = "test";
        stubStreamAnnotation(streamType, direction, subscribed, name);
    }

    @Test
    @DisplayName("constructor() - Given a streamStore, eventSerializer, and streamNameGenerator, then return a new StreamFactory")
    void constructor() {
        assertNotNull(streamFactory);
    }


    @Test
    @DisplayName("fromStreamAnnotation() - Given a stream annotation, a read model, when fromStreamAnnotation() is called, then return StreamDetails")
    void fromStreamAnnotation() {

        try (MockedStatic<EventHandlerUtils> eventHandlerUtils = mockStatic(EventHandlerUtils.class)) {
            List<Class<DewdropAccountCreated>> events = List.of(DewdropAccountCreated.class);
            eventHandlerUtils.when(() -> EventHandlerUtils.getEventHandlers(readModel)).thenReturn(events);

            StreamDetails streamDetails = streamFactory.fromStreamAnnotation(streamAnnotation, readModel);
            assertThat(streamDetails.getStreamName(), is("$ce-test"));
            assertThat(streamDetails.getStreamType(), is(streamType));
            assertThat(streamDetails.getSubscriptionStartStrategy(), is(SubscriptionStartStrategy.READ_ALL_START_END));
            assertThat(streamDetails.getMessageTypes(), is(events));
            assertThat(streamDetails.getDirection(), is(direction));
            assertThat(streamDetails.isSubscribed(), is(subscribed));
            assertThat(streamDetails.getStartPositionMethod().isEmpty(), is(true));
        }
    }

    @Test
    @DisplayName("fromStreamAnnotation() - Given a stream annotation, a read model, when we do not have an InMemoryCache but we do have a StartPositionMethod, then return a StreamDetails")
    void fromStreamAnnotation_withStartPositionMethod() {

        Method startPositionMethod = mock(Method.class);
        readModel = new ReadModel(ReadModelWrapper.of(DewdropAccountDetailsReadModel.class).get(), Optional.empty());

        try (MockedStatic<EventHandlerUtils> eventHandlerUtils = mockStatic(EventHandlerUtils.class)) {
            try (MockedStatic<StreamUtils> utilities = mockStatic(StreamUtils.class)) {
                List<Class<DewdropAccountCreated>> events = List.of(DewdropAccountCreated.class);
                eventHandlerUtils.when(() -> EventHandlerUtils.getEventHandlers(readModel)).thenReturn(events);
                utilities.when(() -> StreamUtils.getStreamStartPositionMethod(any(Stream.class), any())).thenReturn(Optional.of(startPositionMethod));

                StreamDetails streamDetails = streamFactory.fromStreamAnnotation(streamAnnotation, readModel);

                assertThat(streamDetails.getStreamName(), is("$ce-test"));
                assertThat(streamDetails.getStreamType(), is(streamType));
                assertThat(streamDetails.getSubscriptionStartStrategy(), is(SubscriptionStartStrategy.START_FROM_POSITION));
                assertThat(streamDetails.getMessageTypes(), is(events));
                assertThat(streamDetails.getDirection(), is(direction));
                assertThat(streamDetails.isSubscribed(), is(subscribed));
                assertThat(streamDetails.getStartPositionMethod().isPresent(), is(true));
            }
        }
    }

    @Test
    @DisplayName("fromStreamAnnotation() - Given a stream annotation, a read model, when we do not have an InMemoryCache or a StartPositionMethod, then throw an IllegalStateException")
    void fromStreamAnnotation_noStartPositionMethod() {
        readModel = new ReadModel(ReadModelWrapper.of(DewdropAccountDetailsReadModel.class).get(), Optional.empty());
        try (MockedStatic<StreamUtils> utilities = mockStatic(StreamUtils.class)) {
            utilities.when(() -> StreamUtils.getStreamStartPositionMethod(any(Stream.class), any())).thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class, () -> streamFactory.fromStreamAnnotation(streamAnnotation, readModel));
        }
    }

    @Test
    @DisplayName("fromAggregateRoot() - Given an AggregateRoot and a UUID, when fromAggregateRoot() is called, then return StreamDetails")
    void fromAggregateRoot() {
        AggregateRoot aggregateRoot = new AggregateRoot(new DewdropAccountAggregate());
        UUID id = UUID.randomUUID();
        StreamDetails streamDetails = streamFactory.fromAggregateRoot(aggregateRoot, id);

        assertThat(streamDetails.getStreamName(), is("DewdropAccountAggregate-" + id));
        assertThat(streamDetails.getStreamType(), is(StreamType.AGGREGATE));
        assertThat(streamDetails.getSubscriptionStartStrategy(), is(SubscriptionStartStrategy.READ_ALL_START_END));
        assertThat(streamDetails.getMessageTypes(), is(List.of()));
        assertThat(streamDetails.getDirection(), is(direction));
        assertThat(streamDetails.isSubscribed(), is(subscribed));
        assertThat(streamDetails.getStartPositionMethod().isEmpty(), is(true));
    }

    @Test
    @DisplayName("fromEvent() - Given an AggregateRoot and no UUID, when fromAggregateRoot() is called, then return StreamDetails")
    void fromEvent() {
        Consumer consumer = mock(Consumer.class);
        Class<DewdropAccountCreated> eventClass = DewdropAccountCreated.class;
        StreamDetails streamDetails = streamFactory.fromEvent(consumer, eventClass);

        assertThat(streamDetails.getStreamName(), is("$et-DewdropAccountCreated"));
        assertThat(streamDetails.getStreamType(), is(StreamType.EVENT));
        assertThat(streamDetails.getSubscriptionStartStrategy(), is(SubscriptionStartStrategy.START_END_ONLY));
        assertThat(streamDetails.getMessageTypes(), is(List.of(eventClass)));
        assertThat(streamDetails.getDirection(), is(direction));
        assertThat(streamDetails.isSubscribed(), is(subscribed));
        assertThat(streamDetails.getStartPositionMethod().isEmpty(), is(true));
    }

    @Test
    @DisplayName("constructStreamFromAggregateRoot() - Given an AggregateRoot and a UUID, when constructStreamFromAggregateRoot() is called, then return a Stream")
    void constructStreamFromAggregateRoot() {
        StreamDetails streamDetails = mock(StreamDetails.class);
        doReturn(streamDetails).when(streamFactory).fromAggregateRoot(any(), any());
        org.dewdrop.read.readmodel.stream.Stream stream = streamFactory.constructStreamFromAggregateRoot(new AggregateRoot(new DewdropAccountAggregate()), UUID.randomUUID());
        assertThat(stream.getStreamDetails(), is(streamDetails));
        assertThat(stream.getEventSerializer(), is(eventSerializer));
        assertThat(stream.getStreamStore(), is(streamStore));
    }

    @Test
    @DisplayName("constructStreamFromStream() - Given an AggregateRoot and a UUID, when constructStreamFromStream() is called, then return a Stream")
    void constructStreamFromStream() {
        StreamDetails streamDetails = mock(StreamDetails.class);
        doReturn(streamDetails).when(streamFactory).fromStreamAnnotation(any(Stream.class), any(ReadModel.class));
        org.dewdrop.read.readmodel.stream.Stream stream = streamFactory.constructStreamFromStream(streamAnnotation, readModel);
        assertThat(stream.getStreamDetails(), is(streamDetails));
        assertThat(stream.getEventSerializer(), is(eventSerializer));
        assertThat(stream.getStreamStore(), is(streamStore));
    }

    @Test
    @DisplayName("constructStreamForEvent() - Given a Consumer and an Event class, when constructStreamFromStream() is called, then return a Stream")
    void constructStreamForEvent() {
        Consumer consumer = mock(Consumer.class);
        Class<DewdropAccountCreated> eventClass = DewdropAccountCreated.class;
        StreamDetails streamDetails = mock(StreamDetails.class);
        doReturn(streamDetails).when(streamFactory).fromEvent(any(Consumer.class), any(Class.class));
        org.dewdrop.read.readmodel.stream.Stream stream = streamFactory.constructStreamForEvent(consumer, eventClass);
        assertThat(stream.getStreamDetails(), is(streamDetails));
        assertThat(stream.getEventSerializer(), is(eventSerializer));
        assertThat(stream.getStreamStore(), is(streamStore));
    }

    private void stubStreamAnnotation(StreamType streamType, Direction direction, boolean subscribed, String name) {
        doReturn(streamType).when(streamAnnotation).streamType();
        doReturn(direction).when(streamAnnotation).direction();
        doReturn(subscribed).when(streamAnnotation).subscribed();
        doReturn(name).when(streamAnnotation).name();
    }

    @Stream(streamType = StreamType.CATEGORY, direction = Direction.FORWARD, subscribed = true, name = "test")
    private class TestReadModel {
        public TestReadModel() {}
    }
}

