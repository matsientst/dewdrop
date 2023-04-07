package events.dewdrop.datastore.eventstore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.eventstore.dbclient.Position;
import com.eventstore.dbclient.ReadResult;
import com.eventstore.dbclient.ReadStreamOptions;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.dbclient.Subscription;
import com.eventstore.dbclient.SubscriptionListener;
import events.dewdrop.streamstore.eventstore.EventStoreUtils;
import events.dewdrop.structure.events.ReadEventData;
import events.dewdrop.structure.events.StreamReadResults;
import events.dewdrop.structure.read.Direction;
import events.dewdrop.structure.read.ReadRequest;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class EventStoreUtilsTest {
    String streamName = "test";
    long start = 1L;
    long count = 2L;
    Direction forward = Direction.FORWARD;
    RecordedEvent recordedEvent = mock(RecordedEvent.class);
    private String eventStreamId;
    private Long streamRevision;
    private UUID eventId;
    private Position position;
    private HashMap<String, String> systemMetadata;
    private byte[] eventData;
    private byte[] userMetadata;
    private long now;

    @BeforeEach
    public void setup() {
        eventStreamId = UUID.randomUUID().toString();
        streamRevision = 3L;
        eventId = UUID.randomUUID();
        position = new Position(4L, 3L);
        systemMetadata = new HashMap<>();
        now = Instant.now().toEpochMilli();
        systemMetadata.put("created", now + "");
        systemMetadata.put("type", "TestEvent");
        eventData = "eventData".getBytes();
        userMetadata = "userMetadata".getBytes();

        recordedEvent = recordedEvent(eventStreamId, streamRevision, eventId, position, systemMetadata, eventData, userMetadata);
    }

    @Test
    void toStreamReadResults() {
        ReadRequest readRequest = new ReadRequest(streamName, start, count, forward);

        List<ResolvedEvent> resolvedEvents = List.of(new ResolvedEvent(recordedEvent, recordedEvent, mock(Position.class)));

        ReadResult readResult = mock(ReadResult.class);
        doReturn(resolvedEvents).when(readResult).getEvents();

        StreamReadResults streamReadResults = EventStoreUtils.toStreamReadResults(readRequest, readResult);

        assertThat(readRequest.getStreamName(), is(streamReadResults.getStreamName()));
        assertThat(readRequest.getStart(), is(streamReadResults.getFromEventNumber()));
        assertThat(readRequest.getDirection(), is(streamReadResults.getDirection()));
        ReadEventData event = streamReadResults.getEvents().get(0);
        assertThat(eventId, is(event.getEventId()));
        assertThat(streamRevision + 1, is(streamReadResults.getNextEventPosition()));
        assertThat(streamRevision, is(streamReadResults.getLastEventPosition()));
    }

    @Test
    void toReadEventData() {

        ReadEventData readEventData = EventStoreUtils.toReadEventData(recordedEvent);

        assertThat(readEventData.getEventId(), is(eventId));
        assertThat(readEventData.getEventType(), is("TestEvent"));
        assertThat(readEventData.isJson(), is(true));
        assertThat(readEventData.getData().length, is(eventData.length));
        assertThat(readEventData.getMetadata().length, is(userMetadata.length));
        assertThat(readEventData.getEventStreamId(), is(eventStreamId));
        assertThat(readEventData.getEventNumber(), is(streamRevision));

        // TODO: Eventstore code seems to be wrong
        // assertThat(readEventData.getCreatedEpoch(), is(now));
    }

    @Test
    void options() {
        ReadRequest readRequest = new ReadRequest(streamName, start, count, forward);

        ReadStreamOptions options = EventStoreUtils.options(readRequest);
        // TODO: Why are these package level reads now?
        // assertThat(options.getStartingRevision().getValueUnsigned(), is(start));
        // assertThat(options.getDirection(), is(com.eventstore.dbclient.Direction.Forwards));
        // assertThat(options.shouldResolveLinkTos(), is(true));
    }

    @Test
    @DisplayName("createListener() - Test creation of the listener")
    void createListener() {
        Consumer eventAppeared = recordedEvent -> assertThat(recordedEvent, is(Matchers.notNullValue()));
        Subscription subscription = mock(Subscription.class);
        doReturn(UUID.randomUUID().toString()).when(subscription).getSubscriptionId();
        ResolvedEvent resolvedEvent = mock(ResolvedEvent.class);

        RecordedEvent eventDbRecordedEvent = recordedEvent(eventStreamId, streamRevision, eventId, position, systemMetadata, eventData, userMetadata);

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

    private RecordedEvent recordedEvent(String eventStreamId, Long streamRevision, UUID eventId, Position position, HashMap<String, String> systemMetadata, byte[] eventData, byte[] userMetadata) {
        try {
            Constructor<RecordedEvent> constructor = (Constructor<RecordedEvent>) RecordedEvent.class.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            recordedEvent = constructor.newInstance(eventStreamId, streamRevision, eventId, position, systemMetadata, eventData, userMetadata);
        } catch (Exception e) {
            fail("unable to create RecordedEvent");
        }
        return recordedEvent;
    }

}
