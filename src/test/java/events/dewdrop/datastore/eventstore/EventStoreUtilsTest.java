package events.dewdrop.datastore.eventstore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import events.dewdrop.streamstore.eventstore.EventStoreUtils;
import events.dewdrop.structure.events.ReadEventData;
import events.dewdrop.structure.events.StreamReadResults;
import events.dewdrop.structure.read.Direction;
import events.dewdrop.structure.read.ReadRequest;
import com.eventstore.dbclient.Position;
import com.eventstore.dbclient.ReadResult;
import com.eventstore.dbclient.ReadStreamOptions;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.dbclient.StreamRevision;
import com.eventstore.dbclient.Subscription;
import com.eventstore.dbclient.SubscriptionListener;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class EventStoreUtilsTest {
    String streamName = "test";
    long start = 1L;
    long count = 2L;
    Direction forward = Direction.FORWARD;

    @Test
    void toStreamReadResults() {
        ReadRequest readRequest = new ReadRequest(streamName, start, count, forward);

        String eventStreamId = UUID.randomUUID().toString();
        StreamRevision streamRevision = new StreamRevision(3L);
        UUID eventId = UUID.randomUUID();
        Position position = new Position(4L, 3L);
        HashMap<String, String> systemMetadata = new HashMap<>();
        systemMetadata.put("created", Instant.now().toEpochMilli() + "");
        systemMetadata.put("type", "TestEvent");
        byte[] eventData = "eventData".getBytes();
        byte[] userMetadata = "userMetadata".getBytes();

        RecordedEvent eventDbRecordedEvent = new RecordedEvent(eventStreamId, streamRevision, eventId, position, systemMetadata, eventData, userMetadata);
        List<ResolvedEvent> resolvedEvents = List.of(new ResolvedEvent(eventDbRecordedEvent, eventDbRecordedEvent));

        ReadResult readResult = new ReadResult(resolvedEvents);
        StreamReadResults streamReadResults = EventStoreUtils.toStreamReadResults(readRequest, readResult);

        assertThat(readRequest.getStreamName(), is(streamReadResults.getStreamName()));
        assertThat(readRequest.getStart(), is(streamReadResults.getFromEventNumber()));
        assertThat(readRequest.getDirection(), is(streamReadResults.getDirection()));
        ReadEventData event = streamReadResults.getEvents().get(0);
        assertThat(eventId, is(event.getEventId()));
        assertThat(streamRevision.getValueUnsigned() + 1, is(streamReadResults.getNextEventPosition()));
        assertThat(streamRevision.getValueUnsigned(), is(streamReadResults.getLastEventPosition()));
    }

    @Test
    void toReadEventData() {
        String eventStreamId = UUID.randomUUID().toString();
        StreamRevision streamRevision = new StreamRevision(3L);
        UUID eventId = UUID.randomUUID();
        Position position = new Position(4L, 3L);
        HashMap<String, String> systemMetadata = new HashMap<>();

        long now = System.currentTimeMillis();
        systemMetadata.put("created", (now + ""));
        systemMetadata.put("type", "TestEvent");
        byte[] eventData = "eventData".getBytes();
        byte[] userMetadata = "userMetadata".getBytes();

        RecordedEvent eventDbRecordedEvent = new RecordedEvent(eventStreamId, streamRevision, eventId, position, systemMetadata, eventData, userMetadata);

        ReadEventData readEventData = EventStoreUtils.toReadEventData(eventDbRecordedEvent);

        assertThat(readEventData.getEventId(), is(eventId));
        assertThat(readEventData.getEventType(), is("TestEvent"));
        assertThat(readEventData.isJson(), is(true));
        assertThat(readEventData.getData().length, is(eventData.length));
        assertThat(readEventData.getMetadata().length, is(userMetadata.length));
        assertThat(readEventData.getEventStreamId(), is(eventStreamId));
        assertThat(readEventData.getEventNumber(), is(streamRevision.getValueUnsigned()));

        // TODO: This seems like a bug. The timestamp does not get returned correctly. This looks to be in
        // the Eventstore code.
        // assertThat("Test", is(readEventData.getCreated()));
        // assertThat(Instant.now().toEpochMilli(), is(readEventData.getCreatedEpoch()));
    }

    @Test
    void options() {
        ReadRequest readRequest = new ReadRequest(streamName, start, count, forward);

        ReadStreamOptions options = EventStoreUtils.options(readRequest);

        assertThat(options.getStartingRevision().getValueUnsigned(), is(start));
        assertThat(options.getDirection(), is(com.eventstore.dbclient.Direction.Forwards));
        assertThat(options.shouldResolveLinkTos(), is(true));
    }

    @Test
    @DisplayName("createListener() - Test creation of the listener")
    void createListener() {
        Consumer eventAppeared = recordedEvent -> assertThat(recordedEvent, is(Matchers.notNullValue()));
        Subscription subscription = mock(Subscription.class);
        doReturn(UUID.randomUUID().toString()).when(subscription).getSubscriptionId();
        ResolvedEvent resolvedEvent = mock(ResolvedEvent.class);
        String eventStreamId = UUID.randomUUID().toString();
        StreamRevision streamRevision = new StreamRevision(3L);
        UUID eventId = UUID.randomUUID();
        Position position = new Position(4L, 3L);
        HashMap<String, String> systemMetadata = new HashMap<>();
        systemMetadata.put("created", Instant.now().toEpochMilli() + "");
        systemMetadata.put("type", "TestEvent");
        byte[] eventData = "eventData".getBytes();
        byte[] userMetadata = "userMetadata".getBytes();

        RecordedEvent eventDbRecordedEvent = new RecordedEvent(eventStreamId, streamRevision, eventId, position, systemMetadata, eventData, userMetadata);

        doReturn(eventDbRecordedEvent).when(resolvedEvent).getEvent();
        doReturn(eventDbRecordedEvent).when(resolvedEvent).getLink();
        try (MockedStatic<EventStoreUtils> utilities = mockStatic(EventStoreUtils.class)) {
            utilities.when(() -> EventStoreUtils.toReadEventData(any(RecordedEvent.class))).thenReturn(mock(ReadEventData.class));

        }
        SubscriptionListener listener = EventStoreUtils.createListener(eventAppeared);
        listener.onEvent(subscription, resolvedEvent);

        listener.onCancelled(subscription);
        IllegalArgumentException exception = new IllegalArgumentException("", new RuntimeException("Bad things happened"));
        listener.onError(subscription, exception);
    }
}
