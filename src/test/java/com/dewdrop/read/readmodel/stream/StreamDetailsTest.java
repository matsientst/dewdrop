package com.dewdrop.read.readmodel.stream;

import static com.dewdrop.read.readmodel.stream.StreamType.AGGREGATE;
import static com.dewdrop.read.readmodel.stream.StreamType.CATEGORY;
import static com.dewdrop.read.readmodel.stream.StreamType.EVENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.fixture.automated.DewdropUserAggregate;
import com.dewdrop.fixture.events.DewdropAccountCreated;
import com.dewdrop.fixture.events.DewdropUserEvent;
import com.dewdrop.streamstore.stream.PrefixStreamNameGenerator;
import com.dewdrop.structure.StreamNameGenerator;
import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.read.Direction;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class StreamDetailsTest {
    Consumer<Event> handler = mock(Consumer.class);
    StreamNameGenerator streamNameGenerator = new PrefixStreamNameGenerator();
    List<Class<? extends Event>> messageTypes = List.of(DewdropUserEvent.class, DewdropAccountCreated.class);
    StreamType streamType = CATEGORY;
    StreamDetails streamDetails = StreamDetails.builder().streamType(streamType).direction(Direction.FORWARD).eventHandler(handler).streamNameGenerator(streamNameGenerator).messageTypes(messageTypes).name("Test").subscribed(true).create();

    @Test
    void builder() {
        assertThat(streamDetails.getStreamName(), is("$ce-Test"));
        assertThat(streamDetails.getStreamType(), is(CATEGORY));
        assertThat(streamDetails.getMessageTypes(), is(messageTypes));
        assertThat(streamDetails.getDirection(), is(Direction.FORWARD));
        assertThat(streamDetails.getStreamNameGenerator(), is(streamNameGenerator));
        assertThat(streamDetails.getSubscriptionStartStrategy(), is(SubscriptionStartStrategy.READ_ALL_START_END));
    }

    @Test
    void builder_event() {
        StreamType streamType = EVENT;
        StreamDetails streamDetails = StreamDetails.builder().streamType(streamType).direction(Direction.FORWARD).eventHandler(handler).streamNameGenerator(streamNameGenerator).messageTypes(messageTypes).name("Test").subscribed(true).create();
        assertThat(streamDetails.getStreamName(), is("$et-Test"));
        assertThat(streamDetails.getStreamType(), is(EVENT));
    }

    @Test
    void builder_aggregate() {
        StreamType streamType = AGGREGATE;
        UUID id = UUID.randomUUID();
        StreamDetails streamDetails = StreamDetails.builder().streamType(streamType).direction(Direction.FORWARD).eventHandler(handler).streamNameGenerator(streamNameGenerator).messageTypes(messageTypes).name("Test")
                        .aggregateRoot(new AggregateRoot()).id(id).subscribed(true).create();
        assertThat(streamDetails.getStreamName(), is("AggregateRoot-" + id));
        assertThat(streamDetails.getStreamType(), is(AGGREGATE));
    }

    @Test
    void builder_aggregate_withId() {
        StreamType streamType = AGGREGATE;
        UUID id = UUID.randomUUID();
        DewdropUserAggregate target = new DewdropUserAggregate();
        target.setUserId(id);
        AggregateRoot aggregateRoot = new AggregateRoot(target);

        StreamDetails streamDetails = StreamDetails.builder().streamType(streamType).direction(Direction.FORWARD).eventHandler(handler).streamNameGenerator(streamNameGenerator).messageTypes(messageTypes).name("Test").aggregateRoot(aggregateRoot)
                        .subscribed(true).create();
        assertThat(streamDetails.getStreamName(), is("DewdropUserAggregate-" + id));
        assertThat(streamDetails.getStreamType(), is(AGGREGATE));
    }

    @Test
    void builder_aggregate_withoutId() {
        StreamType streamType = AGGREGATE;
        DewdropUserAggregate target = new DewdropUserAggregate();
        AggregateRoot aggregateRoot = new AggregateRoot(target);

        assertThrows(IllegalArgumentException.class, () -> StreamDetails.builder().streamType(streamType).direction(Direction.FORWARD).eventHandler(handler).streamNameGenerator(streamNameGenerator).messageTypes(messageTypes).name("Test")
                        .aggregateRoot(aggregateRoot).subscribed(true).create());
    }


    @Test
    void getMessageTypeNames() {
        assertThat(streamDetails.getMessageTypeNames(), is("DewdropUserEvent, DewdropAccountCreated"));
    }
}
