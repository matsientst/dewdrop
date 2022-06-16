package com.dewdrop.read;

import static java.util.stream.Collectors.toList;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import com.dewdrop.structure.NoStreamException;
import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.events.ReadEventData;
import com.dewdrop.structure.events.StreamReadResults;
import com.dewdrop.structure.read.Direction;
import com.dewdrop.structure.read.ReadRequest;
import com.dewdrop.structure.serialize.EventSerializer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Data
@Log4j2
public class StreamReader {
    private static final int READ_PAGE_SIZE = 500;
    protected AtomicLong streamPosition;
    protected boolean firstEventRead = false;
    protected String streamName;
    protected StreamDetails streamDetails;

    private StreamStore streamStore;
    private EventSerializer eventSerializer;
    private boolean streamExists = false;
    private NameAndPosition nameAndPosition;

    public StreamReader(StreamStore streamStore, EventSerializer eventSerializer, StreamDetails streamDetails, AtomicLong streamPosition) {
        this(streamStore, eventSerializer, streamDetails);
        this.streamPosition = streamPosition;
    }

    public StreamReader(StreamStore streamStore, EventSerializer eventSerializer, StreamDetails streamDetails) {
        this.streamStore = streamStore;
        this.eventSerializer = eventSerializer;
        this.streamDetails = streamDetails;
        this.streamName = streamDetails.getStreamName();
        this.streamPosition = new AtomicLong(0);
        this.nameAndPosition = NameAndPosition.builder().streamType(streamDetails.getStreamType()).name(streamDetails.getStreamName()).consumer(streamDetails.getEventHandler()).create();
    }


    public boolean read(Long start, Long count) {
        long sliceStart = Optional.ofNullable(start).orElse(streamDetails.getDirection() == Direction.FORWARD ? -1L : 0L);
        long remaining = Optional.ofNullable(count).orElse(Long.MAX_VALUE);
        log.debug("Reading from:{} starting at position:{} and ending at:{}", streamDetails.getStreamName(), sliceStart, remaining);
        StreamReadResults readResults;
        do {
            long page = remaining < READ_PAGE_SIZE ? remaining : READ_PAGE_SIZE;

            ReadRequest request = new ReadRequest(streamName, sliceStart, page, streamDetails.getDirection());
            readResults = streamStore.read(request);
            if (!readResults.isStreamExists()) {
                this.streamExists = false;
                return false;
            }
            this.streamExists = true;
            this.firstEventRead = true;
            remaining -= readResults.getEvents().size();
            sliceStart = readResults.getNextEventPosition();

            readResults.getEvents().forEach(eventRead());
            streamPosition.setRelease(readResults.getNextEventPosition());

        } while (!readResults.isEndOfStream() && remaining != 0);
        return this.firstEventRead;
    }

    protected Consumer<ReadEventData> eventRead() {
        return readEventData -> {
            try {
                while (true) {
                    long existingValue = streamPosition.get();
                    long newValue = readEventData.getEventNumber();
                    if (streamPosition.compareAndSet(existingValue, newValue)) {
                        break;
                    }
                }

                this.firstEventRead = true;

                Optional<Event> event = eventSerializer.deserialize(readEventData);
                streamDetails.getEventHandler().accept((Message) event.get());
            } catch (Exception e) {
                log.error("problem reading event: ", e);
            }

        };
    }

    public boolean validateStreamName(String streamName) {
        ReadRequest request = new ReadRequest(streamName, 0L, 1L, Direction.FORWARD);
        StreamReadResults readResults = streamStore.read(request);
        this.streamExists = readResults.isStreamExists();
        return this.streamExists;
    }

    public Long getPosition() {
        return this.firstEventRead ? this.streamPosition.get() : 0L;
    }

    public NameAndPosition nameAndPosition() throws NoStreamException {
        try {
            Long position = getPosition();
            if (validateStreamName(streamName)) {
                read(getPosition(), null);
                return nameAndPosition.completeTask(streamName, getPosition());
            }
            return nameAndPosition;
        } catch (Exception e) {
            log.error("There was a problem reading from: {}", streamName, e);
            return nameAndPosition;
        }
    }

    public AggregateRoot getById(StreamStoreGetByIDRequest getByIDRequest) {
        AggregateRoot aggregateRoot = getByIDRequest.getAggregateRoot();
        log.debug("Getting by ID for aggregateRoot:{}, with ID:{}", aggregateRoot.getTargetClassName(), getByIDRequest.getId());
        int version = getByIDRequest.getVersion();
        if (version <= 0) { throw new IllegalArgumentException("Cannot get version <= 0"); }
        if (getByIDRequest.getCommand() != null) {
            aggregateRoot.setSource(getByIDRequest.getCommand());
        }

        Long sliceStart = 0L;
        StreamReadResults streamReadResults;
        Long appliedEventCount = 0L;
        do {
            Long sliceCount = sliceStart + READ_PAGE_SIZE <= version ? READ_PAGE_SIZE : version - sliceStart;
            ReadRequest request = new ReadRequest(streamName, sliceStart, sliceCount, Direction.FORWARD);
            streamReadResults = streamStore.read(request);

            if (!streamReadResults.isStreamExists()) { return aggregateRoot; }
            // if (streamReadResults instanceof StreamNotFoundSlice) {throw new AggregateNotFoundException(id,
            // aggClass);}
            //
            // if (streamReadResults instanceof StreamDeletedSlice) {throw new AggregateDeletedException(id,
            // aggClass);}

            sliceStart = streamReadResults.getNextEventPosition();

            appliedEventCount += streamReadResults.getEvents().size();
            List<Message> messages = streamReadResults.getEvents().stream().map(evt -> {
                Optional<Event> deserialize = eventSerializer.deserialize(evt);
                if (deserialize.isPresent()) { return deserialize.get(); }
                return null;
            }).filter(e -> e != null).collect(toList());
            aggregateRoot.restoreFromEvents(messages);

        } while (version > streamReadResults.getNextEventPosition() && !streamReadResults.isEndOfStream());
        //
        // if (version != Integer.MAX_VALUE && version != appliedEventCount) {throw new
        // AggregateVersionException(id, aggClass, (long) version, aggregate.getExpectedVersion());}
        //
        // if (version != Integer.MAX_VALUE && aggregate.getExpectedVersion() != version - 1) {throw new
        // AggregateVersionException(id, aggClass, (long) version, aggregate.getExpectedVersion());}

        return aggregateRoot;
    }
}
