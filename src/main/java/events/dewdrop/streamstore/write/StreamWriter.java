package events.dewdrop.streamstore.write;

import events.dewdrop.aggregate.AggregateRoot;
import events.dewdrop.read.readmodel.stream.StreamDetails;
import events.dewdrop.structure.datastore.StreamStore;
import events.dewdrop.structure.events.WriteEventData;
import events.dewdrop.structure.serialize.EventSerializer;
import events.dewdrop.structure.write.WriteRequest;
import events.dewdrop.utils.AggregateIdUtils;
import events.dewdrop.structure.api.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class StreamWriter {

    protected StreamDetails streamDetails;
    private StreamStore streamStore;
    private EventSerializer eventSerializer;

    public static final String AGGREGATE_CLR_TYPE_NAME = "aggregateClassName";
    public static final String COMMIT_ID_HEADER = "commitId";
    public static final String MESSAGE_ID = "messageId";
    public static final String CORRELATION_ID = "correlationId";
    public static final String CAUSATION_ID = "causationId";
    private static final int READ_PAGE_SIZE = 500;


    private StreamWriter(StreamDetails streamDetails, StreamStore streamStore, EventSerializer eventSerializer) {
        this.streamDetails = streamDetails;
        this.streamStore = streamStore;
        this.eventSerializer = eventSerializer;
    }

    public static StreamWriter getInstance(StreamDetails streamDetails, StreamStore streamStore, EventSerializer eventSerializer) {
        return new StreamWriter(streamDetails, streamStore, eventSerializer);
    }

    public void save(AggregateRoot aggregateRoot) {
        Optional<UUID> aggregateId = AggregateIdUtils.getAggregateId(aggregateRoot.getTarget());

        if (aggregateId.isEmpty()) { throw new IllegalArgumentException("There is no aggregateId to persist"); }

        long expectedVersion = aggregateRoot.getVersion();
        List<Message> newMessages = aggregateRoot.takeEvents();
        List<WriteEventData> eventsToSave = generateEventsToSave(aggregateRoot, newMessages);
        WriteRequest request = new WriteRequest(streamDetails.getStreamName(), expectedVersion, eventsToSave);
        streamStore.appendToStream(request);
    }

    Map<String, Object> commitHeaders(AggregateRoot aggregateRoot) {
        Map<String, Object> commitHeaders = new HashMap<>();
        commitHeaders.put(COMMIT_ID_HEADER, UUID.randomUUID());
        commitHeaders.put(AGGREGATE_CLR_TYPE_NAME, aggregateRoot.getTargetClassName());

        if (aggregateRoot.getCausationId() != null) {
            commitHeaders.put(CAUSATION_ID, aggregateRoot.getCausationId());
        }
        if (aggregateRoot.getCorrelationId() != null) {
            commitHeaders.put(CORRELATION_ID, aggregateRoot.getCorrelationId());
        }
        return commitHeaders;
    }

    List<WriteEventData> generateEventsToSave(AggregateRoot aggregateRoot, List<Message> newMessages) {
        Map<String, Object> commitHeaders = commitHeaders(aggregateRoot);
        List<WriteEventData> eventsToSave = new ArrayList<>();
        for (Message message : newMessages) {
            Optional<WriteEventData> serializedAggregate = eventSerializer.serialize(message, new HashMap<>(commitHeaders));
            if (serializedAggregate.isEmpty()) { throw new IllegalStateException("Failed to serialize event: " + message.getClass().getSimpleName()); }
            eventsToSave.add(serializedAggregate.get());
        }
        return eventsToSave;
    }
}
