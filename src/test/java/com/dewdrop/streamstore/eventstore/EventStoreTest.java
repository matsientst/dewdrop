package com.dewdrop.streamstore.eventstore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dewdrop.structure.NoStreamException;
import com.dewdrop.structure.events.StreamReadResults;
import com.dewdrop.structure.events.WriteEventData;
import com.dewdrop.structure.read.Direction;
import com.dewdrop.structure.read.ReadRequest;
import com.dewdrop.structure.subscribe.SubscribeRequest;
import com.dewdrop.structure.write.WriteRequest;
import com.eventstore.dbclient.AppendToStreamOptions;
import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadResult;
import com.eventstore.dbclient.ReadStreamOptions;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.dbclient.StreamNotFoundException;
import com.eventstore.dbclient.StreamRevision;
import com.eventstore.dbclient.SubscribeToStreamOptions;
import com.eventstore.dbclient.SubscriptionListener;
import com.eventstore.dbclient.proto.persistentsubscriptions.Persistent.CreateReq.StreamOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.apache.commons.collections4.ArrayStack;
import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class EventStoreTest {

    EventStore eventStore;
    EventStoreDBClient eventStoreDBClient;
    RecordedEvent recordedEvent;
    ReadResult readResult;
    StreamReadResults streamReadResults;
    ReadRequest readRequest;

    @BeforeEach
    void setup() {
        eventStoreDBClient = mock(EventStoreDBClient.class);
        eventStore = spy(new EventStore(eventStoreDBClient));
        recordedEvent = mock(RecordedEvent.class);
        readResult = new ReadResult(List.of(mock(ResolvedEvent.class)));
        streamReadResults = mock(StreamReadResults.class);
        readRequest = mock(ReadRequest.class);
    }

    @Test
    void read() {
        doReturn(streamReadResults).when(eventStore).readFromStream(any(ReadRequest.class));
        StreamReadResults streamReadResults = eventStore.read(mock(ReadRequest.class));

        assertThat(streamReadResults, isA(StreamReadResults.class));
        verify(eventStore, times(1)).readFromStream(any(ReadRequest.class));
    }

    @Test
    void readFromStream() {
        try (MockedStatic<EventStoreUtils> utils = mockStatic(EventStoreUtils.class)) {
            utils.when(() -> EventStoreUtils.toStreamReadResults(any(ReadRequest.class), any(ReadResult.class))).thenReturn(mock(StreamReadResults.class));

            doReturn(Optional.of(readResult)).when(eventStore).performRead(any(ReadRequest.class));

            StreamReadResults streamReadResults = eventStore.readFromStream(mock(ReadRequest.class));

            assertThat(streamReadResults, isA(StreamReadResults.class));
        }
    }

    @Test
    void readFromStream_with_empty_readResult() {
        try (MockedStatic<EventStoreUtils> utils = mockStatic(EventStoreUtils.class)) {
            utils.when(() -> EventStoreUtils.toStreamReadResults(any(ReadRequest.class), any(ReadResult.class))).thenReturn(mock(StreamReadResults.class));

            doReturn(Optional.empty()).when(eventStore).performRead(any(ReadRequest.class));

            StreamReadResults streamReadResults = eventStore.read(mock(ReadRequest.class));

            assertThat(streamReadResults, is(StreamReadResults.empty()));
        }
    }

    @Test
    void readFromStream_with_no_stream_exception() {
        try (MockedStatic<EventStoreUtils> utils = mockStatic(EventStoreUtils.class)) {
            utils.when(() -> EventStoreUtils.toStreamReadResults(any(ReadRequest.class), any(ReadResult.class))).thenReturn(mock(StreamReadResults.class));

            doThrow(NoStreamException.class).when(eventStore).performRead(any(ReadRequest.class));

            StreamReadResults streamReadResults = eventStore.read(mock(ReadRequest.class));

            assertThat(streamReadResults, is(StreamReadResults.noStream()));
        }
    }

    @Test
    void performRead() throws ExecutionException, InterruptedException {
        String streamName = "streamName";
        Long start = 1L;
        Long count = 1L;

        CompletableFuture completableFuture = mock(CompletableFuture.class);

        ReadRequest readRequest = new ReadRequest(streamName, start, count, Direction.FORWARD);

        try (MockedStatic<EventStoreUtils> utils = mockStatic(EventStoreUtils.class)) {
            utils.when(() -> EventStoreUtils.options(any(ReadRequest.class))).thenReturn(mock(ReadStreamOptions.class));

            doReturn(completableFuture).when(eventStoreDBClient).readStream(anyString(), anyLong(), any(ReadStreamOptions.class));

            doReturn(readResult).when(completableFuture).get();

            Optional<ReadResult> readResult = eventStore.performRead(readRequest);

            assertThat(readResult.isPresent(), is(true));
        }
    }

    @Test
    void performRead_interrupted_exception() throws ExecutionException, InterruptedException {
        String streamName = "streamName";
        Long start = 1L;
        Long count = 1L;

        CompletableFuture completableFuture = mock(CompletableFuture.class);

        ReadRequest readRequest = new ReadRequest(streamName, start, count, Direction.FORWARD);

        try (MockedStatic<EventStoreUtils> utils = mockStatic(EventStoreUtils.class)) {
            utils.when(() -> EventStoreUtils.options(any(ReadRequest.class))).thenReturn(mock(ReadStreamOptions.class));


            doReturn(completableFuture).when(eventStoreDBClient).readStream(anyString(), anyLong(), any(ReadStreamOptions.class));

            doThrow(InterruptedException.class).when(completableFuture).get();

            Optional<ReadResult> readResult = eventStore.performRead(readRequest);

            assertThat(readResult.isEmpty(), is(true));
        }
    }

    @Test
    void performRead_execution_exception() throws ExecutionException, InterruptedException {
        String streamName = "streamName";
        Long start = 1L;
        Long count = 1L;

        CompletableFuture completableFuture = mock(CompletableFuture.class);

        ReadRequest readRequest = new ReadRequest(streamName, start, count, Direction.FORWARD);

        try (MockedStatic<EventStoreUtils> utils = mockStatic(EventStoreUtils.class)) {
            utils.when(() -> EventStoreUtils.options(any(ReadRequest.class))).thenReturn(mock(ReadStreamOptions.class));

            doReturn(completableFuture).when(eventStoreDBClient).readStream(anyString(), anyLong(), any(ReadStreamOptions.class));

            doThrow(ExecutionException.class).when(completableFuture).get();

            Optional<ReadResult> readResult = eventStore.performRead(readRequest);

            assertThat(readResult.isEmpty(), is(true));
        }
    }

    @Test
    void subscribeToStream() {
        SubscribeToStreamOptions options = mock(SubscribeToStreamOptions.class);
        SubscribeRequest subscribeRequest = mock(SubscribeRequest.class);

        try (MockedStatic<EventStoreUtils> eventUtils = mockStatic(EventStoreUtils.class)) {
            eventUtils.when(() -> EventStoreUtils.createListener(any(Consumer.class))).thenReturn(mock(SubscriptionListener.class));
        }

        try (MockedStatic<SubscribeToStreamOptions> utils = mockStatic(SubscribeToStreamOptions.class)) {
            utils.when(() -> SubscribeToStreamOptions.get()).thenReturn(options);

        }
        doReturn(mock(StreamRevision.class)).when(options).fromRevision(anyLong());

        doReturn(mock(SubscribeToStreamOptions.class)).when(options).resolveLinkTos();

        doReturn("streamName").when(subscribeRequest).getStreamName();

        doReturn(true).when(eventStore).subscribeTo(anyString(), any(SubscriptionListener.class), any(SubscribeToStreamOptions.class));

        boolean result = eventStore.subscribeToStream(subscribeRequest);

        assertThat(result, is(true));
    }

    @Test
    void subscribeToStream_with_false_subscribeTo() {
        SubscribeToStreamOptions options = mock(SubscribeToStreamOptions.class);
        SubscribeRequest subscribeRequest = mock(SubscribeRequest.class);

        try (MockedStatic<EventStoreUtils> eventUtils = mockStatic(EventStoreUtils.class)) {
            eventUtils.when(() -> EventStoreUtils.createListener(any(Consumer.class))).thenReturn(mock(SubscriptionListener.class));
        }

        try (MockedStatic<SubscribeToStreamOptions> utils = mockStatic(SubscribeToStreamOptions.class)) {
            utils.when(() -> SubscribeToStreamOptions.get()).thenReturn(options);

        }
        doReturn(mock(StreamRevision.class)).when(options).fromRevision(anyLong());

        doReturn(mock(SubscribeToStreamOptions.class)).when(options).resolveLinkTos();

        doReturn("streamName").when(subscribeRequest).getStreamName();

        doReturn(false).when(eventStore).subscribeTo(anyString(), any(SubscriptionListener.class), any(SubscribeToStreamOptions.class));

        boolean result = eventStore.subscribeToStream(subscribeRequest);

        assertThat(result, is(false)); // TODO: this test is for one line of code, is it needed?
    }

    @Test
    void subscribeTo() throws ExecutionException, InterruptedException {

        doReturn(mock(CompletableFuture.class)).when(eventStoreDBClient).subscribeToStream(anyString(), any(SubscriptionListener.class), any(SubscribeToStreamOptions.class));

        boolean result = eventStore.subscribeTo("streamName", mock(SubscriptionListener.class), mock(SubscribeToStreamOptions.class));

        assertThat(result, is(true));
    }

    @Test
    void subscribeTo_Interupted_Exception() throws ExecutionException, InterruptedException {
        boolean result = true;
        CompletableFuture completableFuture = mock(CompletableFuture.class);
        doReturn(completableFuture).when(eventStoreDBClient).subscribeToStream(anyString(), any(SubscriptionListener.class), any(SubscribeToStreamOptions.class));

        doThrow(InterruptedException.class).when(completableFuture).get();

        result = eventStore.subscribeTo("streamName", mock(SubscriptionListener.class), mock(SubscribeToStreamOptions.class));

        assertThat(result, is(false));
    }

    @Test
    void subscribeTo_Execution_Exception_with_StreamNotFound() throws ExecutionException, InterruptedException {
        boolean result = true;
        CompletableFuture completableFuture = mock(CompletableFuture.class);
        ExecutionException exception = new ExecutionException("", new StreamNotFoundException());

        doReturn(completableFuture).when(eventStoreDBClient).subscribeToStream(anyString(), any(SubscriptionListener.class), any(SubscribeToStreamOptions.class));

        doThrow(exception).when(completableFuture).get();

        result = eventStore.subscribeTo("streamName", mock(SubscriptionListener.class), mock(SubscribeToStreamOptions.class));

        assertThat(result, is(false));
    }

    @Test
    void subscribeTo_Execution_Exception_without_StreamNotFound() throws ExecutionException, InterruptedException {
        boolean result = true;
        CompletableFuture completableFuture = mock(CompletableFuture.class);
        ExecutionException exception = new ExecutionException("", new NullPointerException());

        doReturn(completableFuture).when(eventStoreDBClient).subscribeToStream(anyString(), any(SubscriptionListener.class), any(SubscribeToStreamOptions.class));

        doThrow(exception).when(completableFuture).get();

        try {
            eventStore.subscribeTo("streamName", mock(SubscriptionListener.class), mock(SubscribeToStreamOptions.class));
        } catch (RuntimeException e) {
            assertThat(e, is(instanceOf(RuntimeException.class)));
        }
    }

    @Test
    void appendToStream() {
        Integer currentBatchSize = eventStore.BATCH_SIZE + 1;
        WriteRequest writeRequest = mock(WriteRequest.class);
        List list = spy(new ArrayList());
        doReturn(list).when(writeRequest).getEvents();
        doReturn("streamName").when(writeRequest).getStreamName();
        doReturn(currentBatchSize).when(list).size();
        try (MockedStatic<ListUtils> listUtils = mockStatic(ListUtils.class)) {
            List<EventData> eventData = spy(new ArrayList<>());
            listUtils.when(() -> ListUtils.partition(any(List.class), anyInt())).thenReturn(List.of(List.of(eventData)));
            doReturn(mock(ListIterator.class)).when(eventData).listIterator();

            doReturn(mock(CompletableFuture.class)).when(eventStoreDBClient).appendToStream(anyString(), any(AppendToStreamOptions.class), any(ListIterator.class));

            eventStore.appendToStream(writeRequest);
            verify(eventStoreDBClient, times(1)).appendToStream(anyString(), any(AppendToStreamOptions.class), any(ListIterator.class));
        }
    }

    @Test
    void appendToStream_InterruptedException() throws ExecutionException, InterruptedException {
        Integer currentBatchSize = eventStore.BATCH_SIZE;
        WriteRequest writeRequest = mock(WriteRequest.class);
        List list = spy(new ArrayList());
        doReturn(list).when(writeRequest).getEvents();
        doReturn("streamName").when(writeRequest).getStreamName();
        try (MockedStatic<ListUtils> listUtils = mockStatic(ListUtils.class)) {
            List<EventData> eventData = spy(new ArrayList());
            listUtils.when(() -> ListUtils.partition(any(List.class), anyInt())).thenReturn(List.of(List.of(eventData)));
            doReturn(mock(ListIterator.class)).when(eventData).listIterator();

            CompletableFuture completableFuture = mock(CompletableFuture.class);
            doReturn(completableFuture).when(eventStoreDBClient).appendToStream(anyString(), any(AppendToStreamOptions.class), any(ListIterator.class));

            doThrow(InterruptedException.class).when(completableFuture).get();

            eventStore.appendToStream(writeRequest);
            // TODO: verify log message?
        }
    }

    @Test
    void appendToStream_ExecutionException() throws ExecutionException, InterruptedException {
        Integer currentBatchSize = eventStore.BATCH_SIZE;
        WriteRequest writeRequest = mock(WriteRequest.class);
        doReturn(new ArrayList()).when(writeRequest).getEvents();
        doReturn("streamName").when(writeRequest).getStreamName();
        ExecutionException exception = new ExecutionException("", new NullPointerException());
        try (MockedStatic<ListUtils> listUtils = mockStatic(ListUtils.class)) {
            List<EventData> eventData = spy(new ArrayList());
            listUtils.when(() -> ListUtils.partition(any(List.class), anyInt())).thenReturn(List.of(List.of(eventData)));
            doReturn(mock(ListIterator.class)).when(eventData).listIterator();

            CompletableFuture completableFuture = mock(CompletableFuture.class);
            doReturn(completableFuture).when(eventStoreDBClient).appendToStream(anyString(), any(AppendToStreamOptions.class), any(ListIterator.class));

            doThrow(exception).when(completableFuture).get();

            eventStore.appendToStream(writeRequest);
            // TODO: verify log message?
        }
    }


}
