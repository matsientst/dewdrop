package com.dewdrop.streamstore.repository;

import static java.util.stream.Collectors.toList;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.streamstore.stream.PrefixStreamNameGenerator;
import com.dewdrop.structure.StreamNameGenerator;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.events.CorrelationCausation;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.events.StreamReadResults;
import com.dewdrop.structure.events.WriteEventData;
import com.dewdrop.structure.read.Direction;
import com.dewdrop.structure.read.ReadRequest;
import com.dewdrop.structure.serialize.EventSerializer;
import com.dewdrop.structure.write.WriteRequest;
import com.dewdrop.utils.AggregateIdUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Data
@Log4j2
public class StreamStoreRepository {

    StreamStore connection;
    StreamNameGenerator streamNameGenerator;
    EventSerializer serializer;

    public static final String AGGREGATE_CLR_TYPE_NAME = "AggregateClrTypeName";
    public static final String AGGREGATE_CLR_TYPE_NAME_HEADER = "AggregateClrTypeNameHeader";
    public static final String COMMIT_ID_HEADER = "CommitId";

    public static final String MESSAGE_ID = "msgId";
    public static final String CORRELATION_ID = "correlationId";
    public static final String CAUSATION_ID = "causationId";

    private static final int READ_PAGE_SIZE = 500;

    public StreamStoreRepository() {
        this.streamNameGenerator = new PrefixStreamNameGenerator();
    }

    public StreamStoreRepository(StreamStore connection, StreamNameGenerator streamNameGenerator, EventSerializer serializer) {
        this.connection = connection;
        this.streamNameGenerator = streamNameGenerator;
        this.serializer = serializer;
    }


    public Optional<AggregateRoot> getById(UUID id, AggregateRoot aggregateRoot, int version, CorrelationCausation command) {
        log.debug("Looking up aggregate with id {}", id);
        if (version <= 0) {throw new IllegalArgumentException("Cannot get version <= 0");}

        String streamName = streamNameGenerator.generateForAggregate(aggregateRoot.getTarget().getClass(), id);

        Long sliceStart = 0L;
        StreamReadResults streamReadResults;
        Long appliedEventCount = 0L;
        do {
            Long sliceCount = sliceStart + READ_PAGE_SIZE <= version ? READ_PAGE_SIZE : version - sliceStart;
            ReadRequest request = new ReadRequest(streamName, sliceStart, sliceCount, Direction.FORWARD);
            streamReadResults = connection.read(request);

            if (!streamReadResults.isStreamExists()) {
                return Optional.empty();
            }
            // if (streamReadResults instanceof StreamNotFoundSlice) {throw new AggregateNotFoundException(id,
            // aggClass);}
            //
            // if (streamReadResults instanceof StreamDeletedSlice) {throw new AggregateDeletedException(id,
            // aggClass);}

            sliceStart = streamReadResults.getNextEventNumber();

            appliedEventCount += streamReadResults.getEvents()
                .size();
            List<Message> messages = streamReadResults.getEvents()
                .stream()
                .map(evt -> {
                    Optional<Message> deserialize = serializer.deserialize(evt);
                    if (deserialize.isPresent()) {return deserialize.get();}
                    return null;
                })
                .filter(e -> e != null)
                .collect(toList());
            aggregateRoot.restoreFromEvents(messages);

        } while (version > streamReadResults.getNextEventNumber() && !streamReadResults.isEndOfStream());
        //
        // if (version != Integer.MAX_VALUE && version != appliedEventCount) {throw new
        // AggregateVersionException(id, aggClass, (long) version, aggregate.getExpectedVersion());}
        //
        // if (version != Integer.MAX_VALUE && aggregate.getExpectedVersion() != version - 1) {throw new
        // AggregateVersionException(id, aggClass, (long) version, aggregate.getExpectedVersion());}
        if (command != null) {
            aggregateRoot.setSource(command);
        }

        return Optional.of(aggregateRoot);
    }

    AggregateRoot constructFromAggregate(Class<? extends AggregateRoot> aggClass) {
        AggregateRoot aggregate;
        try {
            Constructor<? extends AggregateRoot> declaredConstructor = aggClass.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            aggregate = declaredConstructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException(String.format("Unable to construct AggregateRoot of type %s", aggClass.getSimpleName()));
        }
        return aggregate;
    }


    @SuppressWarnings("java:S125")
//    public void update(AggregateRoot aggregate, int version) {
//        if (aggregate == null || aggregate.getId() == null) { throw new IllegalArgumentException("Invalid null aggregate"); }
//        if (version == aggregate.getExpectedVersion()) { return; }
//        if (version <= 0) { throw new IllegalArgumentException("Cannot get version <= 0"); }
//        if (version < aggregate.getExpectedVersion()) { throw new IllegalArgumentException("Aggregate is ahead of version"); }
//
//        var streamName = streamNameGenerator.generateForAggregate(aggregate.getClass(), aggregate.getId());
//        long sliceStart = aggregate.getExpectedVersion() + 1;
//        StreamReadResults currentSlice;
//        do {
//            long sliceCount = sliceStart + READ_PAGE_SIZE <= version ? READ_PAGE_SIZE : version - sliceStart;
//            ReadRequest request = new ReadRequest(streamName, sliceStart, sliceCount, Direction.FORWARD);
//            currentSlice = connection.read(request);
//
//            // if (currentSlice instanceof StreamNotFoundSlice) {throw new
//            // AggregateNotFoundException(aggregate.getId(), aggregate.getClass());}
//            //
//            // if (currentSlice instanceof StreamDeletedSlice) {throw new
//            // AggregateDeletedException(aggregate.getId(), aggregate.getClass());}
//
//            sliceStart = currentSlice.getNextEventNumber();
//
//            List<Event> events = (List) currentSlice.getEvents().stream().map(evt -> serializer.deserialize(evt)).collect(toList());
//            aggregate.updateWithEvents(events, aggregate.getExpectedVersion());
//
//        } while (version > currentSlice.getNextEventNumber() && !currentSlice.isEndOfStream());
//
//        // if (version != Integer.MAX_VALUE && aggregate.getExpectedVersion() != version - 1)
//        // throw new AggregateVersionException(aggregate.getId(), aggregate.getClass(), (long) version,
//        // aggregate.getExpectedVersion());
//    }
//

    public void save(AggregateRoot aggregateRoot) {
        Map<String, Object> commitHeaders = new HashMap<>();
        commitHeaders.put(COMMIT_ID_HEADER, UUID.randomUUID());
        String header = aggregateRoot.getClass()
            .getName() + "," + aggregateRoot.getClass()
            .getSimpleName();
        commitHeaders.put(AGGREGATE_CLR_TYPE_NAME_HEADER, header);
        commitHeaders.put(AGGREGATE_CLR_TYPE_NAME, aggregateRoot.getClass()
            .getName());

        if (aggregateRoot.getCorrelationId() != null) {
            commitHeaders.put(CAUSATION_ID, aggregateRoot.getCausationId());
        }
        if (aggregateRoot.getCorrelationId() != null) {
            commitHeaders.put(CORRELATION_ID, aggregateRoot.getCorrelationId());
        }

        Object target = aggregateRoot.getTarget();

        Optional<UUID> aggregateId = AggregateIdUtils.getAggregateId(target);

        if (aggregateId.isEmpty()) {
            throw new IllegalArgumentException("There is no aggregateId to persist");
        }

        String streamName = streamNameGenerator.generateForAggregate(target.getClass(), aggregateId.get());
        long expectedVersion = aggregateRoot.getVersion();
        List<Message> newMessages = aggregateRoot.takeEvents();
        List<WriteEventData> eventsToSave = getEventsToSave(commitHeaders, newMessages);
        WriteRequest request = new WriteRequest(streamName, expectedVersion, eventsToSave);
        connection.appendToStream(request);
    }

    List<WriteEventData> getEventsToSave(Map<String, Object> commitHeaders, List<Message> newMessages) {
        List<WriteEventData> eventsToSave = new ArrayList<>();
        for (Message message : newMessages) {
            Optional<WriteEventData> serializedAggregate = serializer.serialize(message, new HashMap<>(commitHeaders));
            if (serializedAggregate.isEmpty()) {throw new IllegalStateException("Failed to serialize event " + message);}
            eventsToSave.add(serializedAggregate.get());

        }
        return eventsToSave;
    }
}
