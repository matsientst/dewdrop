package com.dewdropper.read;

import com.dewdropper.structure.NoStreamException;
import com.dewdropper.structure.datastore.StreamStore;
import com.dewdropper.structure.api.Message;
import com.dewdropper.structure.events.ReadEventData;
import com.dewdropper.structure.events.StreamReadResults;
import com.dewdropper.structure.read.Direction;
import com.dewdropper.structure.read.ReadRequest;
import com.dewdropper.structure.serialize.EventSerializer;
import com.dewdropper.utils.WaitUntilUtils;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Data
@Log4j2
public class StreamReader {
    private static final int READ_PAGE_SIZE = 500;
    protected AtomicLong streamPosition = new AtomicLong(0L);
    protected boolean firstEventRead = false;
    protected String streamName;
    protected StreamDetails streamDetails;

    private StreamStore streamStoreConnection;
    private Class<?> eventType;
    private EventSerializer serializer;
    private Consumer<Message> onEvent;

    public StreamReader(StreamStore connection, EventSerializer serializer, Consumer<Message> onEvent, StreamDetails streamDetails, Class<?> eventType) {
        this.streamStoreConnection = connection;
        this.eventType = eventType;
        this.serializer = serializer;
        this.onEvent = onEvent;
        this.streamDetails = streamDetails;
        this.streamName = streamDetails.getStreamName();
    }

    public boolean read(Long checkpoint, Long count, Direction direction) throws NoStreamException {
        if (checkpoint != null && checkpoint < 0) { throw new IllegalArgumentException("A negative checkpoint:" + checkpoint + " is not allowed"); }

        if (count != null && count < 1) { throw new IllegalArgumentException("A non positive count:" + count + " is not allowed"); }

        if (!validateStreamName(streamName)) { throw new NoStreamException(streamName); }

        this.firstEventRead = false;
        direction = Optional.ofNullable(direction).orElse(Direction.FORWARD);
        long sliceStart = Optional.ofNullable(checkpoint).orElse(direction == Direction.FORWARD ? -1L : 0L);
        long remaining = Optional.ofNullable(count).orElse(Long.MAX_VALUE);
        StreamReadResults readResults;
        do {
            long page = remaining < READ_PAGE_SIZE ? remaining : READ_PAGE_SIZE;

            ReadRequest request = new ReadRequest(streamName, sliceStart, page, direction);
            readResults = streamStoreConnection.read(request);

            this.firstEventRead = true;
            remaining -= readResults.getEvents().size();
            sliceStart = readResults.getNextEventNumber();

            readResults.getEvents().forEach(eventRead());

        } while (!readResults.isEndOfStream() && remaining != 0);
        return this.firstEventRead;
    }

    boolean isReadComplete(BooleanSupplier completionCheck) {
        if (this.firstEventRead && completionCheck != null) {
            try {
                // Is this right?
                WaitUntilUtils.waitUntil(completionCheck, 200);
            } catch (TimeoutException e) {
                log.error("Timeout! ", e);
                return true;
            }

            return true;
        }
        return false;
    }

    protected Consumer<ReadEventData> eventRead() {
        return recordedEvent -> {
            try {
                while (true) {
                    long existingValue = streamPosition.get();
                    long newValue = recordedEvent.getEventNumber();
                    if (streamPosition.compareAndSet(existingValue, newValue)) {
                        break;
                    }
                }

                this.firstEventRead = true;

                Object event = serializer.deserialize(recordedEvent);
                if (event instanceof Message) {
                    onEvent.accept((Message) event);
                }
            } catch (Exception e) {
                log.error("problem reading event: ", e);
            }

        };
    }

    public boolean validateStreamName(String streamName) {
        StreamReadResults currentSlice = null;
        try {
            ReadRequest request = new ReadRequest(streamName, 0L, 1L, Direction.FORWARD);
            currentSlice = streamStoreConnection.read(request);
        } catch (NoStreamException e) {
            return false;
        }
        return !(currentSlice.isEmpty());
    }

    public Long getPosition() {
        return this.firstEventRead ? this.streamPosition.get() : null;
    }

    public NameAndPosition getNameAndPosition() throws NoStreamException {
        String simpleName = eventType.getSimpleName();

        NameAndPosition nameAndPosition = NameAndPosition.builder().streamType(streamDetails.getStreamType()).name(streamDetails.getStreamName()).consumer(onEvent).messageType(eventType).create();

        try {
            Long position = getPosition();
            return nameAndPosition.completeTask(streamName, position);
        } catch (NoStreamException e) {
            throw e;
        } catch (Exception e) {
            log.error("There was a problem reading from: {} for: {}", streamName, simpleName, e);
            return nameAndPosition;
        }
    }
}
