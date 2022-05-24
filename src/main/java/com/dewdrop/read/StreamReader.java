package com.dewdrop.read;

import com.dewdrop.structure.NoStreamException;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.events.ReadEventData;
import com.dewdrop.structure.events.StreamReadResults;
import com.dewdrop.structure.read.Direction;
import com.dewdrop.structure.read.ReadRequest;
import com.dewdrop.structure.serialize.EventSerializer;
import com.dewdrop.utils.WaitUntilUtils;
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
    protected AtomicLong streamPosition;
    protected boolean firstEventRead = false;
    protected String streamName;
    protected StreamDetails streamDetails;

    private StreamStore streamStore;
    private EventSerializer eventSerializer;

    public StreamReader(StreamStore streamStore, EventSerializer eventSerializer, StreamDetails streamDetails, AtomicLong streamPosition) {
        this.streamStore = streamStore;
        this.eventSerializer = eventSerializer;
        this.streamDetails = streamDetails;
        this.streamName = streamDetails.getStreamName();
        this.streamPosition = streamPosition;
    }

    public StreamReader(StreamStore streamStore, EventSerializer eventSerializer, StreamDetails streamDetails) {
        this.streamStore = streamStore;
        this.eventSerializer = eventSerializer;
        this.streamDetails = streamDetails;
        this.streamName = streamDetails.getStreamName();
        this.streamPosition = new AtomicLong(0);
    }

    public boolean read(Long start, Long count) throws NoStreamException {
        if (!validateStreamName(streamName)) {throw new NoStreamException(streamName);}

        long sliceStart = Optional.ofNullable(start)
            .orElse(streamDetails.getDirection() == Direction.FORWARD ? -1L : 0L);
        long remaining = Optional.ofNullable(count)
            .orElse(Long.MAX_VALUE);
        log.debug("Reading from:{} starting at position:{} and ending at:{}", streamDetails.getStreamName(), sliceStart, remaining);
        StreamReadResults readResults;
        do {
            long page = remaining < READ_PAGE_SIZE ? remaining : READ_PAGE_SIZE;

            ReadRequest request = new ReadRequest(streamName, sliceStart, page, streamDetails.getDirection());
            readResults = streamStore.read(request);

            this.firstEventRead = true;
            remaining -= readResults.getEvents()
                .size();
            sliceStart = readResults.getNextEventPosition();

            readResults.getEvents()
                .forEach(eventRead());
            streamPosition.setRelease(readResults.getNextEventPosition());

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

                Optional<Object> event = eventSerializer.deserialize(readEventData);
                if (event.get() instanceof Message) {
                    streamDetails.getEventHandler()
                        .accept((Message) event.get());
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
            currentSlice = streamStore.read(request);
        } catch (NoStreamException e) {
            return false;
        }
        return !(currentSlice.isEmpty());
    }

    public Long getPosition() {
        return this.firstEventRead ? this.streamPosition.get() : -1L;
    }

    public NameAndPosition getNameAndPosition() throws NoStreamException {
        String simpleName = streamDetails.getMessageType()
            .getSimpleName();

        NameAndPosition nameAndPosition = NameAndPosition.builder()
            .streamType(streamDetails.getStreamType())
            .name(streamDetails.getStreamName())
            .consumer(streamDetails.getEventHandler())
            .messageType(streamDetails.getMessageType())
            .create();

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
