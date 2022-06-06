package com.dewdrop.streamstore.eventstore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.dewdrop.structure.events.StreamReadResults;
import com.dewdrop.structure.read.Direction;
import com.dewdrop.structure.read.ReadRequest;
import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadResult;
import com.eventstore.dbclient.ReadStreamOptions;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventStoreTest {

    EventStore eventStore;
    EventStoreDBClient eventStoreDBClient;
    RecordedEvent recordedEvent;

    @BeforeEach
    void setup() {
        eventStoreDBClient = mock(EventStoreDBClient.class);
        eventStore = spy(new EventStore(eventStoreDBClient));
        recordedEvent = mock(RecordedEvent.class);

        //

    }

    @Test
    void read() {
        String streamName = "streamName";
        Long start = 0L;
        Long count = 1L;
        Direction direction = Direction.FORWARD;
        ReadRequest readRequest = new ReadRequest(streamName, start, count, direction);
//        ReadResult readResult = new ReadResult(List.of(mock(ResolvedEvent.class)));
        CompletableFuture readResult = CompletableFuture.completedFuture(new ReadResult(List.of(mock(ResolvedEvent.class))));
        doReturn(readResult).when(eventStoreDBClient)
            .readStream(anyString(), anyLong(), any(ReadStreamOptions.class));
        StreamReadResults streamReadResults = eventStore.read(readRequest);

        assertThat(streamReadResults, is(Optional.of(readResult)));
    }
    // Need: RecordedEvent, ResolvedEvent

}