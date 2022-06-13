package com.dewdrop.streamstore.eventstore;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;


import com.eventstore.dbclient.Position;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import org.junit.jupiter.api.Test;

class EventStoreTestUtilsTest {

    @Test
    void createRecordedEvent() {
        assertThat(EventStoreTestUtils.buildRecordedEvent().position(new Position(1L, 0L)).build(), isA(RecordedEvent.class));
    }

    @Test
    void createResolvedEvent() {
        RecordedEvent recordedEvent = EventStoreTestUtils.buildRecordedEvent().position(new Position(1L, 0L)).build();
        Position position = new Position(2L, 1L);
        assertThat(EventStoreTestUtils.buildResolvedEvent().recordedEvent(recordedEvent).position(position).build(), isA(ResolvedEvent.class));
    }

}
