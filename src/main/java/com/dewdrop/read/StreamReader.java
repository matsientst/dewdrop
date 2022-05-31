package com.dewdrop.read;

import com.dewdrop.structure.NoStreamException;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.events.ReadEventData;
import com.dewdrop.structure.events.StreamReadResults;
import com.dewdrop.structure.read.Direction;
import com.dewdrop.structure.read.ReadRequest;
import com.dewdrop.structure.serialize.EventSerializer;
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
        this.nameAndPosition = NameAndPosition.builder()
            .streamType(streamDetails.getStreamType())
            .name(streamDetails.getStreamName())
            .consumer(streamDetails.getEventHandler())
            .create();
    }


    public boolean read(Long start, Long count) {
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
            if (!readResults.isStreamExists()) {
                this.streamExists = false;
                return false;
            }
            this.streamExists = true;
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
}
