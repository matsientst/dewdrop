package events.dewdrop.read.readmodel.stream;

import static java.util.stream.Collectors.toList;

import events.dewdrop.aggregate.AggregateRoot;
import events.dewdrop.api.result.Result;
import events.dewdrop.structure.NoStreamException;
import events.dewdrop.structure.events.StreamReadResults;
import events.dewdrop.structure.read.ReadRequest;
import events.dewdrop.utils.DependencyInjectionUtils;
import events.dewdrop.utils.DewdropReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import events.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import events.dewdrop.structure.api.Event;
import events.dewdrop.structure.api.Message;
import events.dewdrop.structure.datastore.StreamStore;
import events.dewdrop.structure.events.ReadEventData;
import events.dewdrop.structure.read.Direction;
import events.dewdrop.structure.serialize.EventSerializer;

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

    private StreamReader(StreamStore streamStore, EventSerializer eventSerializer, StreamDetails streamDetails) {
        this.streamStore = streamStore;
        this.eventSerializer = eventSerializer;
        this.streamDetails = streamDetails;
        this.streamName = streamDetails.getStreamName();
        this.streamPosition = new AtomicLong(0);
        this.nameAndPosition = NameAndPosition.builder().streamType(streamDetails.getStreamType()).name(streamDetails.getStreamName()).create();
    }

    public static StreamReader getInstance(StreamStore streamStore, EventSerializer eventSerializer, StreamDetails streamDetails, AtomicLong streamPosition) {
        StreamReader streamReader = new StreamReader(streamStore, eventSerializer, streamDetails);
        streamReader.setStreamPosition(streamPosition);
        return streamReader;
    }

    public static StreamReader getInstance(StreamStore streamStore, EventSerializer eventSerializer, StreamDetails streamDetails) {
        return new StreamReader(streamStore, eventSerializer, streamDetails);
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

            readResults.getEvents().forEach(this::eventRead);
            streamPosition.setRelease(readResults.getNextEventPosition());

        } while (!readResults.isEndOfStream() && remaining != 0);
        return this.firstEventRead;
    }

    protected void eventRead(ReadEventData readEventData) {
        try {
            streamPosition.setRelease(readEventData.getEventNumber());
            this.firstEventRead = true;

            Optional<Event> event = eventSerializer.deserialize(readEventData);
            if (event.isPresent()) {
                streamDetails.getEventHandler().accept(event.get());
            }
        } catch (Exception e) {
            log.error("problem reading event - eventType:{}", readEventData.getEventType(), e);
        }
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
        switch (streamDetails.getSubscriptionStartStrategy()) {
            case START_FROM_POSITION:
                return readFromPosition();
            case START_END_ONLY:
                return startFromEnd();
            case READ_ALL_START_END:
            default:
                return readAll();
        }
    }

    NameAndPosition readFromPosition() {
        Optional<Method> startPositionMethod = streamDetails.getStartPositionMethod();
        if (startPositionMethod.isEmpty()) { throw new IllegalStateException("startPositionMethod is not set"); }

        Method method = startPositionMethod.get();
        Optional<Object> instance = DependencyInjectionUtils.getInstance(method.getDeclaringClass());
        if (instance.isPresent()) {
            Result<Long> position = DewdropReflectionUtils.callMethod(instance.get(), method);
            if (position.isValuePresent()) {
                streamPosition.set(position.get());
                firstEventRead = true;
                return nameAndPosition.completeTask(streamName, getPosition());
            }
        }

        return nameAndPosition;
    }

    NameAndPosition startFromEnd() {
        ReadRequest request = new ReadRequest(streamName, 0L, 1L, Direction.BACKWARD);
        StreamReadResults readResults = streamStore.read(request);
        streamPosition.set(readResults.getLastEventPosition());
        if (!readResults.isStreamExists()) { return nameAndPosition; }
        return nameAndPosition.completeTask(streamName, getPosition());
    }

    NameAndPosition readAll() {
        try {
            return nameAndPosition.completeTask(streamName, 0L);
        } catch (Exception e) {
            log.error("There was a problem reading from: {}", streamName, e);
            return nameAndPosition;
        }
    }

    public AggregateRoot getById(StreamStoreGetByIDRequest getByIDRequest) {
        AggregateRoot aggregateRoot = getByIDRequest.getAggregateRoot();
        log.debug("Getting by ID for aggregateRoot:{}, with ID:{}", aggregateRoot.getTargetClassName(), getByIDRequest.getId());
        // int version = getByIDRequest.getVersion();
        // if (version <= 0) { throw new IllegalArgumentException("Cannot get version <= 0"); }
        if (getByIDRequest.getCommand() != null) {
            aggregateRoot.setSource(getByIDRequest.getCommand());
        }

        long sliceStart = 0L;
        StreamReadResults streamReadResults;
        long appliedEventCount = 0L;
        int loopCount = 0;

        do {
            // Long sliceCount = sliceStart + READ_PAGE_SIZE <= version ? READ_PAGE_SIZE : version - sliceStart;
            long sliceCount = sliceStart + READ_PAGE_SIZE;
            ReadRequest request = new ReadRequest(streamName, sliceStart, sliceCount, Direction.FORWARD);
            log.info("Reading from:{} starting at position:{} and ending at:{}", streamName, sliceStart, sliceCount);
            streamReadResults = streamStore.read(request);

            if (!streamReadResults.isStreamExists()) { return aggregateRoot; }

            sliceStart = streamReadResults.getNextEventPosition();
            appliedEventCount += streamReadResults.getEvents().size();
            List<Message> messages = streamReadResults.getEvents().stream().map(evt -> {
                Optional<Event> deserialize = eventSerializer.deserialize(evt);
                return deserialize.orElse(null);
            }).filter(e -> e != null).collect(toList());
            aggregateRoot.restoreFromEvents(messages);
            log.info("nextEventPosition:{}, endOfStream:{}", streamReadResults.getNextEventPosition(), streamReadResults.isEndOfStream());

            loopCount++;
        } while (!streamReadResults.isEndOfStream());

        log.info("Total loop count: {}", loopCount);

        return aggregateRoot;
    }

    boolean moreToRead(long version, long nextEventPosition, boolean isEndOfStream) {
        return version > nextEventPosition && !isEndOfStream;
    }
}
