package events.dewdrop.read.readmodel.stream;

import events.dewdrop.aggregate.AggregateRoot;
import events.dewdrop.fixture.automated.DewdropUserAggregate;
import events.dewdrop.fixture.events.DewdropAccountCreated;
import events.dewdrop.fixture.events.DewdropUserEvent;
import events.dewdrop.streamstore.stream.PrefixStreamNameGenerator;
import events.dewdrop.structure.StreamNameGenerator;
import events.dewdrop.structure.api.Event;
import events.dewdrop.structure.read.Direction;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class StreamDetailsTest {
    Consumer<Event> handler = mock(Consumer.class);
    StreamNameGenerator streamNameGenerator = new PrefixStreamNameGenerator();
    List<Class<? extends Event>> messageTypes = List.of(DewdropUserEvent.class, DewdropAccountCreated.class);
    StreamType streamType = StreamType.CATEGORY;
    StreamDetails streamDetails = StreamDetails.builder().streamType(streamType).direction(Direction.FORWARD).eventHandler(handler).streamNameGenerator(streamNameGenerator).messageTypes(messageTypes).name("Test").subscribed(true).create();

    @Test
    void builder() {
        assertThat(streamDetails.getStreamName(), is("$ce-Test"));
        assertThat(streamDetails.getStreamType(), Matchers.is(StreamType.CATEGORY));
        assertThat(streamDetails.getMessageTypes(), is(messageTypes));
        assertThat(streamDetails.getDirection(), is(Direction.FORWARD));
        assertThat(streamDetails.getStreamNameGenerator(), is(streamNameGenerator));
        assertThat(streamDetails.getStartPositionMethod().isEmpty(), is(true));
        assertThat(streamDetails.getSubscriptionStartStrategy(), is(SubscriptionStartStrategy.READ_ALL_START_END));
    }

    @Test
    void builder_event() {
        StreamType streamType = StreamType.EVENT;
        StreamDetails streamDetails = StreamDetails.builder().streamType(streamType).direction(Direction.FORWARD).eventHandler(handler).streamNameGenerator(streamNameGenerator).messageTypes(messageTypes).name("Test").subscribed(true).create();
        assertThat(streamDetails.getStreamName(), is("$et-Test"));
        assertThat(streamDetails.getStreamType(), Matchers.is(StreamType.EVENT));
    }

    @Test
    void builder_aggregate() {
        StreamType streamType = StreamType.AGGREGATE;
        UUID id = UUID.randomUUID();
        StreamDetails streamDetails = StreamDetails.builder().streamType(streamType).direction(Direction.FORWARD).eventHandler(handler).streamNameGenerator(streamNameGenerator).messageTypes(messageTypes).name("Test")
                        .aggregateName(new AggregateRoot().getTarget().getClass().getSimpleName()).id(id).subscribed(true).create();
        assertThat(streamDetails.getStreamName(), is("AggregateRoot-" + id));
        assertThat(streamDetails.getStreamType(), Matchers.is(StreamType.AGGREGATE));
    }

    @Test
    void builder_aggregate_withId() {
        StreamType streamType = StreamType.AGGREGATE;
        UUID id = UUID.randomUUID();
        DewdropUserAggregate target = new DewdropUserAggregate();
        target.setUserId(id);
        AggregateRoot aggregateRoot = new AggregateRoot(target);

        StreamDetails streamDetails = StreamDetails.builder().streamType(streamType).direction(Direction.FORWARD).eventHandler(handler).streamNameGenerator(streamNameGenerator).messageTypes(messageTypes).name("Test")
                        .aggregateName(aggregateRoot.getTarget().getClass().getSimpleName()).id(id).subscribed(true).create();
        assertThat(streamDetails.getStreamName(), is("DewdropUserAggregate-" + id));
        assertThat(streamDetails.getStreamType(), Matchers.is(StreamType.AGGREGATE));
    }

    @Test
    void builder_withStartPositionMethod() {
        StreamDetails streamDetails = StreamDetails.builder().streamType(streamType).direction(Direction.FORWARD).eventHandler(handler).streamNameGenerator(streamNameGenerator).messageTypes(messageTypes).name("Test").subscribed(true)
                        .startPositionMethod(Optional.of(mock(Method.class))).create();
        assertThat(streamDetails.getStartPositionMethod().isPresent(), is(true));
        assertThat(streamDetails.getSubscriptionStartStrategy(), is(SubscriptionStartStrategy.START_FROM_POSITION));
    }

    @Test
    void getMessageTypeNames() {
        assertThat(streamDetails.getMessageTypeNames(), is("DewdropUserEvent, DewdropAccountCreated"));
    }
}
