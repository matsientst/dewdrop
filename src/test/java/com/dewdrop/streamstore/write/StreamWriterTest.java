package com.dewdrop.streamstore.write;

import static com.dewdrop.streamstore.write.StreamWriter.AGGREGATE_CLR_TYPE_NAME;
import static com.dewdrop.streamstore.write.StreamWriter.CAUSATION_ID;
import static com.dewdrop.streamstore.write.StreamWriter.COMMIT_ID_HEADER;
import static com.dewdrop.streamstore.write.StreamWriter.CORRELATION_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.fixture.automated.DewdropUserAggregate;
import com.dewdrop.fixture.command.DewdropCreateUserCommand;
import com.dewdrop.fixture.events.DewdropUserCreated;
import com.dewdrop.read.StreamDetails;
import com.dewdrop.streamstore.eventstore.EventStore;
import com.dewdrop.streamstore.serialize.JsonSerializer;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.events.WriteEventData;
import com.dewdrop.structure.serialize.EventSerializer;
import com.dewdrop.structure.write.WriteRequest;
import com.dewdrop.utils.AggregateIdUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

@Log4j2
class StreamWriterTest {
    StreamWriter streamWriter;
    StreamDetails streamDetails;
    StreamStore streamStore;
    EventSerializer eventSerializer;
    WriteEventData writeEventData;
    AggregateRoot aggregateRoot;
    DewdropUserCreated event;
    DewdropUserAggregate target;

    @BeforeEach
    void setup() {
        this.streamDetails = mock(StreamDetails.class);
        this.streamStore = mock(EventStore.class);
        this.eventSerializer = mock(JsonSerializer.class);
        this.streamWriter = spy(new StreamWriter(streamDetails, streamStore, eventSerializer));
        this.writeEventData = mock(WriteEventData.class);
        this.target = new DewdropUserAggregate();
        this.aggregateRoot = new AggregateRoot(target);
        this.aggregateRoot.setVersion(33L);
        this.event = new DewdropUserCreated(UUID.randomUUID(), "Test");
    }

    @Test
    @DisplayName("save() - Given a valid AggregateRoot, it should save the event to the stream")
    void save() {
        doReturn("Test").when(streamDetails).getStreamName();
        doNothing().when(streamStore).appendToStream(any(WriteRequest.class));
        doReturn(List.of(writeEventData)).when(streamWriter).generateEventsToSave(any(AggregateRoot.class), anyList());
        UUID id = UUID.randomUUID();
        try (MockedStatic<AggregateIdUtils> utilities = mockStatic(AggregateIdUtils.class)) {
            utilities.when(() -> AggregateIdUtils.getAggregateId(any(DewdropUserAggregate.class))).thenReturn(Optional.of(id));
            streamWriter.save(aggregateRoot);

            ArgumentCaptor<WriteRequest> captor = ArgumentCaptor.forClass(WriteRequest.class);
            verify(streamStore, times(1)).appendToStream(captor.capture());

            WriteRequest writeRequest = captor.getValue();
            assertThat(writeRequest.getStreamName(), is("Test"));
            assertThat(writeRequest.getEvents(), is(List.of(writeEventData)));
            assertThat(writeRequest.getExpectedVersion(), is(aggregateRoot.getVersion()));
        }
    }

    @Test
    @DisplayName("save() - Given a valid AggregateRoot with no aggregateRootId, throw an IllegalArgumentException")
    void save_noAggregateId() {
        try (MockedStatic<AggregateIdUtils> utilities = mockStatic(AggregateIdUtils.class)) {
            utilities.when(() -> AggregateIdUtils.getAggregateId(any(DewdropUserAggregate.class))).thenReturn(Optional.empty());
            assertThrows(IllegalArgumentException.class, () -> streamWriter.save(aggregateRoot));
        }
    }

    @Test
    @DisplayName("commitHeaders() - Given a valid AggregateRoot, generate the relevant commit headers")
    void commitHeaders() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "Test");
        command.setCorrelationId(UUID.randomUUID());
        command.setCausationId(UUID.randomUUID());
        aggregateRoot.setSource(command);

        Map<String, Object> commitHeaders = streamWriter.commitHeaders(aggregateRoot);

        assertThat(commitHeaders.get(COMMIT_ID_HEADER), is(notNullValue()));
        assertThat(commitHeaders.get(AGGREGATE_CLR_TYPE_NAME), is(target.getClass().getName()));
        assertThat(commitHeaders.get(CAUSATION_ID), is(aggregateRoot.getCausationId()));
        assertThat(commitHeaders.get(CORRELATION_ID), is(aggregateRoot.getCorrelationId()));
    }

    @Test
    @DisplayName("commitHeaders() - Given a valid AggregateRoot, generate the relevant commit headers")
    void commitHeaders_noCausationOrCorrelationIds() {
        Map<String, Object> commitHeaders = streamWriter.commitHeaders(aggregateRoot);

        assertThat(commitHeaders.get(COMMIT_ID_HEADER), is(notNullValue()));
        assertThat(commitHeaders.get(AGGREGATE_CLR_TYPE_NAME), is(target.getClass().getName()));
        assertThat(commitHeaders.get(CAUSATION_ID), is(nullValue()));
        assertThat(commitHeaders.get(CORRELATION_ID), is(nullValue()));
    }

    @Test
    @DisplayName("generateEventsToSave() - Given a valid aggregate root and a list of events, we should generate a list of events to save, confirm we generate the commitHeaders, serialize the events and return the list")
    void getEventsToSave() {
        doReturn(new HashMap<>()).when(streamWriter).commitHeaders(any(AggregateRoot.class));
        doReturn(Optional.of(writeEventData)).when(eventSerializer).serialize(any(Message.class), anyMap());

        List<WriteEventData> eventData = streamWriter.generateEventsToSave(aggregateRoot, List.of(event));

        assertThat(eventData.size(), is(1));
        assertThat(eventData.get(0), is(writeEventData));
        verify(streamWriter, times(1)).commitHeaders(any(AggregateRoot.class));
        verify(eventSerializer, times(1)).serialize(any(Message.class), anyMap());
    }

    @Test
    @DisplayName("generateEventsToSave() - Given a valid aggregate root but an event we can't deserialize, we should throw an IllegalStateException")
    void getEventsToSave_IllegalStateException() {
        doReturn(Optional.empty()).when(eventSerializer).serialize(any(Message.class), anyMap());
        doReturn(new HashMap()).when(streamWriter).commitHeaders(any(AggregateRoot.class));
        assertThrows(IllegalStateException.class, () -> streamWriter.generateEventsToSave(aggregateRoot, List.of(event)));
    }
}
