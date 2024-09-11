package events.dewdrop.read.readmodel.stream;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import events.dewdrop.aggregate.AggregateRoot;
import events.dewdrop.fixture.automated.DewdropAccountAggregate;
import events.dewdrop.fixture.events.DewdropAccountCreated;
import events.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetailsReadModel;
import events.dewdrop.read.readmodel.ReadModel;
import events.dewdrop.read.readmodel.ReadModelWrapper;
import events.dewdrop.read.readmodel.annotation.Stream;
import events.dewdrop.read.readmodel.cache.InMemoryCacheProcessor;
import events.dewdrop.read.readmodel.cache.MapBackedInMemoryCacheProcessor;
import events.dewdrop.streamstore.stream.PrefixStreamNameGenerator;
import events.dewdrop.structure.StreamNameGenerator;
import events.dewdrop.structure.datastore.StreamStore;
import events.dewdrop.structure.read.Direction;
import events.dewdrop.structure.serialize.EventSerializer;
import events.dewdrop.utils.EventHandlerUtils;
import events.dewdrop.utils.StreamUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class StreamFactoryTest {
    StreamFactory streamFactory;
    StreamStore streamStore;
    EventSerializer eventSerializer;
    StreamNameGenerator streamNameGenerator;
    events.dewdrop.read.readmodel.annotation.Stream streamAnnotation;
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
        streamFactory = Mockito.spy(new StreamFactory(streamStore, eventSerializer, streamNameGenerator));

        streamAnnotation = Mockito.spy(TestReadModel.class.getAnnotation(events.dewdrop.read.readmodel.annotation.Stream.class));
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
                utilities.when(() -> StreamUtils.getStreamStartPositionMethod(anyString(), any(StreamType.class), any())).thenReturn(Optional.of(startPositionMethod));

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
            utilities.when(() -> StreamUtils.getStreamStartPositionMethod(anyString(), any(StreamType.class), any())).thenReturn(Optional.empty());

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
        ReadModel readModel = mock(ReadModel.class);
        ReadModelWrapper readModelWrapper = mock(ReadModelWrapper.class);
        doReturn(readModel.getClass()).when(readModelWrapper).getOriginalReadModelClass();
        doReturn(readModelWrapper).when(readModel).getReadModelWrapper();
        Class<DewdropAccountCreated> eventClass = DewdropAccountCreated.class;
        StreamDetails streamDetails = streamFactory.fromEvent(readModel, eventClass);

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
        events.dewdrop.read.readmodel.stream.Stream stream = streamFactory.constructStreamFromAggregateRoot(new AggregateRoot(new DewdropAccountAggregate()), UUID.randomUUID());
        assertThat(stream.getStreamDetails(), Matchers.is(streamDetails));
        assertThat(stream.getEventSerializer(), is(eventSerializer));
        assertThat(stream.getStreamStore(), is(streamStore));
    }

    @Test
    @DisplayName("constructStreamFromStream() - Given an AggregateRoot and a UUID, when constructStreamFromStream() is called, then return a Stream")
    void constructStreamFromStream() {
        StreamDetails streamDetails = mock(StreamDetails.class);
        doReturn(streamDetails).when(streamFactory).fromStreamAnnotation(any(events.dewdrop.read.readmodel.annotation.Stream.class), any(ReadModel.class));
        events.dewdrop.read.readmodel.stream.Stream stream = streamFactory.constructStreamFromStream(streamAnnotation, readModel);
        assertThat(stream.getStreamDetails(), Matchers.is(streamDetails));
        assertThat(stream.getEventSerializer(), is(eventSerializer));
        assertThat(stream.getStreamStore(), is(streamStore));
    }

    @Test
    @DisplayName("constructStreamForEvent() - Given a Consumer and an Event class, when constructStreamFromStream() is called, then return a Stream")
    void constructStreamForEvent() {
        ReadModel readModel = mock(ReadModel.class);
        Class<DewdropAccountCreated> eventClass = DewdropAccountCreated.class;
        StreamDetails streamDetails = mock(StreamDetails.class);
        doReturn(streamDetails).when(streamFactory).fromEvent(any(ReadModel.class), any(Class.class));
        events.dewdrop.read.readmodel.stream.Stream stream = streamFactory.constructStreamForEvent(readModel, eventClass);
        assertThat(stream.getStreamDetails(), Matchers.is(streamDetails));
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

