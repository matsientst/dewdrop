package events.dewdrop.streamstore.eventstore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
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

import com.eventstore.dbclient.AppendToStreamOptions;
import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadResult;
import com.eventstore.dbclient.ReadStreamOptions;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.dbclient.StreamNotFoundException;
import com.eventstore.dbclient.SubscribeToStreamOptions;
import com.eventstore.dbclient.SubscriptionListener;
import events.dewdrop.structure.NoStreamException;
import events.dewdrop.structure.events.StreamReadResults;
import events.dewdrop.structure.events.WriteEventData;
import events.dewdrop.structure.read.Direction;
import events.dewdrop.structure.read.ReadRequest;
import events.dewdrop.structure.subscribe.SubscribeRequest;
import events.dewdrop.structure.write.WriteRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class EventStoreTest {

    EventStore eventStore;
    EventStoreDBClient eventStoreDBClient;
    RecordedEvent recordedEvent;
    ReadResult readResult;
    StreamReadResults streamReadResults;
    ReadRequest readRequest;
    CompletableFuture<ReadResult> completableFuture;

    @BeforeEach
    void setup() {
        eventStoreDBClient = mock(EventStoreDBClient.class);
        eventStore = Mockito.spy(new EventStore(eventStoreDBClient));
        recordedEvent = mock(RecordedEvent.class);
        readResult = mock(ReadResult.class);
        streamReadResults = mock(StreamReadResults.class);
        readRequest = mock(ReadRequest.class);
        completableFuture = mock(CompletableFuture.class);
        doReturn(List.of(mock(ResolvedEvent.class))).when(readResult).getEvents();
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

            Mockito.doThrow(NoStreamException.class).when(eventStore).performRead(any(ReadRequest.class));

            StreamReadResults streamReadResults = eventStore.read(mock(ReadRequest.class));

            assertThat(streamReadResults, is(StreamReadResults.noStream()));
        }
    }

    @Test
    void performRead() throws ExecutionException, InterruptedException {
        String streamName = "streamName";
        Long start = 1L;
        Long count = 1L;

        ReadRequest readRequest = new ReadRequest(streamName, start, count, Direction.FORWARD);

        try (MockedStatic<EventStoreUtils> utils = mockStatic(EventStoreUtils.class)) {
            utils.when(() -> EventStoreUtils.options(any(ReadRequest.class))).thenReturn(mock(ReadStreamOptions.class));

            doReturn(completableFuture).when(eventStoreDBClient).readStream(anyString(), any(ReadStreamOptions.class));

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

        ReadRequest readRequest = new ReadRequest(streamName, start, count, Direction.FORWARD);

        try (MockedStatic<EventStoreUtils> utils = mockStatic(EventStoreUtils.class)) {
            utils.when(() -> EventStoreUtils.options(any(ReadRequest.class))).thenReturn(mock(ReadStreamOptions.class));

            doReturn(completableFuture).when(eventStoreDBClient).readStream(anyString(), any(ReadStreamOptions.class));

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

        ReadRequest readRequest = new ReadRequest(streamName, start, count, Direction.FORWARD);

        try (MockedStatic<EventStoreUtils> utils = mockStatic(EventStoreUtils.class)) {
            utils.when(() -> EventStoreUtils.options(any(ReadRequest.class))).thenReturn(mock(ReadStreamOptions.class));

            doReturn(completableFuture).when(eventStoreDBClient).readStream(anyString(), any(ReadStreamOptions.class));

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
            utils.when(SubscribeToStreamOptions::get).thenReturn(options);

        }
        // TODO: fix
        // doReturn(mock(StreamRevision.class)).when(options)
        // .fromRevision(anyLong());

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
            utils.when(SubscribeToStreamOptions::get).thenReturn(options);

        }
        // TODO: fix
        // doReturn(mock(StreamRevision.class)).when(options)
        // .fromRevision(anyLong());

        doReturn(mock(SubscribeToStreamOptions.class)).when(options).resolveLinkTos();

        doReturn("streamName").when(subscribeRequest).getStreamName();

        doReturn(false).when(eventStore).subscribeTo(anyString(), any(SubscriptionListener.class), any(SubscribeToStreamOptions.class));

        assertThat(eventStore.subscribeToStream(subscribeRequest), is(false));
    }

    @Test
    void subscribeTo() {

        doReturn(mock(CompletableFuture.class)).when(eventStoreDBClient).subscribeToStream(anyString(), any(SubscriptionListener.class), any(SubscribeToStreamOptions.class));

        boolean result = eventStore.subscribeTo("streamName", mock(SubscriptionListener.class), mock(SubscribeToStreamOptions.class));

        assertThat(result, is(true));
    }

    @Test
    void subscribeTo_Interupted_Exception() throws ExecutionException, InterruptedException {
        doReturn(completableFuture).when(eventStoreDBClient).subscribeToStream(anyString(), any(SubscriptionListener.class), any(SubscribeToStreamOptions.class));

        doThrow(InterruptedException.class).when(completableFuture).get();

        boolean result = eventStore.subscribeTo("streamName", mock(SubscriptionListener.class), mock(SubscribeToStreamOptions.class));

        assertThat(result, is(false));
    }

    @Test
    void subscribeTo_Execution_Exception_with_StreamNotFound() throws ExecutionException, InterruptedException {
        ExecutionException exception = new ExecutionException("", mock(StreamNotFoundException.class));

        doReturn(completableFuture).when(eventStoreDBClient).subscribeToStream(anyString(), any(SubscriptionListener.class), any(SubscribeToStreamOptions.class));

        doThrow(exception).when(completableFuture).get();

        boolean result = eventStore.subscribeTo("streamName", mock(SubscriptionListener.class), mock(SubscribeToStreamOptions.class));

        assertThat(result, is(false));
    }

    @Test
    void subscribeTo_Execution_Exception_without_StreamNotFound() throws ExecutionException, InterruptedException {
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
        Integer currentBatchSize = EventStore.BATCH_SIZE + 1;
        WriteRequest writeRequest = mock(WriteRequest.class);
        List<WriteEventData> list = spy(new ArrayList<>());
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
        WriteRequest writeRequest = mock(WriteRequest.class);
        List<WriteEventData> list = spy(new ArrayList<>());
        doReturn(list).when(writeRequest).getEvents();
        doReturn("streamName").when(writeRequest).getStreamName();
        try (MockedStatic<ListUtils> listUtils = mockStatic(ListUtils.class)) {
            List<EventData> eventData = spy(new ArrayList<>());
            listUtils.when(() -> ListUtils.partition(any(List.class), anyInt())).thenReturn(List.of(List.of(eventData)));
            doReturn(mock(ListIterator.class)).when(eventData).listIterator();

            doReturn(completableFuture).when(eventStoreDBClient).appendToStream(anyString(), any(AppendToStreamOptions.class), any(ListIterator.class));

            doThrow(InterruptedException.class).when(completableFuture).get();

            eventStore.appendToStream(writeRequest);
        }
    }

    @Test
    void appendToStream_ExecutionException() throws ExecutionException, InterruptedException {
        WriteRequest writeRequest = mock(WriteRequest.class);
        doReturn(new ArrayList<>()).when(writeRequest).getEvents();
        doReturn("streamName").when(writeRequest).getStreamName();
        ExecutionException exception = new ExecutionException("", new NullPointerException());
        try (MockedStatic<ListUtils> listUtils = mockStatic(ListUtils.class)) {
            List<EventData> eventData = spy(new ArrayList<>());
            listUtils.when(() -> ListUtils.partition(any(List.class), anyInt())).thenReturn(List.of(List.of(eventData)));
            doReturn(mock(ListIterator.class)).when(eventData).listIterator();

            doReturn(completableFuture).when(eventStoreDBClient).appendToStream(anyString(), any(AppendToStreamOptions.class), any(ListIterator.class));

            doThrow(exception).when(completableFuture).get();

            eventStore.appendToStream(writeRequest);
        }
    }


}
