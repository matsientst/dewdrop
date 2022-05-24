package com.dewdrop.streamstore.eventstore;

import static java.util.stream.Collectors.toList;

import com.dewdrop.structure.NoStreamException;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.events.StreamReadResults;
import com.dewdrop.structure.events.WriteEventData;
import com.dewdrop.structure.read.ReadRequest;
import com.dewdrop.structure.subscribe.SubscribeRequest;
import com.dewdrop.structure.write.WriteRequest;
import com.eventstore.dbclient.AppendToStreamOptions;
import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadResult;
import com.eventstore.dbclient.ReadStreamOptions;
import com.eventstore.dbclient.StreamNotFoundException;
import com.eventstore.dbclient.SubscribeToStreamOptions;
import com.eventstore.dbclient.SubscriptionListener;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;

@Log4j2
public class EventStore implements StreamStore {
    public static final int BATCH_SIZE = 500;
    private final EventStoreDBClient client;

    public EventStore(EventStoreDBClient client) {
        this.client = client;
    }

    @Override
    public StreamReadResults read(ReadRequest readRequest) throws NoStreamException {
        return readFromStream(readRequest);
    }

    @Override
    public void subscribeToStream(SubscribeRequest subscribeRequest) throws NoStreamException {
        SubscriptionListener listener = EventStoreUtils.createListener(subscribeRequest.getConsumeEvent());

        SubscribeToStreamOptions options = SubscribeToStreamOptions.get();
        options.fromRevision(subscribeRequest.getLastCheckpoint());
        options.resolveLinkTos();

        if (subscribeTo(subscribeRequest.getStreamName(), listener, options)) { return; }

        subscribeTo(subscribeRequest.getStreamName(), listener, options);
    }

    boolean subscribeTo(String stream, SubscriptionListener listener, SubscribeToStreamOptions options) throws NoStreamException {
        try {
            client.subscribeToStream(stream, listener, options).get();
            return true;
        } catch (InterruptedException e) {
            log.error("Stream was interrupted - name:" + stream, e);
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            log.error("There was an execution exception - name:" + stream, e);
            if (e.getCause() instanceof StreamNotFoundException) { throw new NoStreamException(stream); }
            return false;
        }
    }

    @Override
    public void appendToStream(WriteRequest writeRequest) {
        AppendToStreamOptions options = AppendToStreamOptions.get().expectedRevision(writeRequest.getExpectedVersion());
        String streamName = writeRequest.getStreamName();
        List<WriteEventData> events = writeRequest.getEvents();
        List<EventData> data = events.stream().map(EventStoreUtils::toEventData).collect(toList());
        try {
            if (events.size() < BATCH_SIZE) {
                ListIterator<EventData> iterator = data.listIterator();
                client.appendToStream(streamName, options, iterator).get();
            } else {
                final List<List<EventData>> batch = ListUtils.partition(data, BATCH_SIZE);

                for (List<EventData> eventDatas : batch) {
                    ListIterator<EventData> iterator = eventDatas.listIterator();
                    client.appendToStream(streamName, options, iterator);
                }
            }
        } catch (InterruptedException e) {
            log.error("Append was interrupted", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("Append had an issue", e);
        }
    }

    private StreamReadResults readFromStream(ReadRequest readRequest) {
        Optional<ReadResult> readResult;

        try {
            readResult = performRead(readRequest);
        } catch (NoStreamException e) {
            log.debug("Stream:{} was not found", readRequest.getStreamName());
            return StreamReadResults.noStream();
        }

        if (readResult.isEmpty()) {
            log.debug("Request had not results for Stream:{} - request:{}", readRequest.getStreamName(), readRequest);
            return StreamReadResults.empty();
        }


        ReadResult result = readResult.get();
        log.debug("Read {} messages from stream:{}", result.getEvents().size(), readRequest.getStreamName());
        return EventStoreUtils.toStreamReadResults(readRequest, result);
    }

    private Optional<ReadResult> performRead(ReadRequest readRequest) throws NoStreamException {
        String streamName = readRequest.getStreamName();
        Long count = readRequest.getCount();
        ReadStreamOptions readStreamOptions = EventStoreUtils.options(readRequest);
        try {
            ReadResult readResult = client.readStream(streamName, count, readStreamOptions).get();
            return Optional.of(readResult);
        } catch (InterruptedException e) {
            log.error("Stream interrupted", e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof StreamNotFoundException) { throw new NoStreamException(streamName); }

            log.error("There was an issue reading from stream: {}", streamName, e);
            return Optional.empty();
        }
    }
}
