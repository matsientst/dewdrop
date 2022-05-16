package com.dewdropper.structure.events;

import com.dewdropper.structure.read.Direction;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Data
public class StreamReadResults {
    private String streamName;
    private long fromEventNumber;
    private Direction direction;
    private List<ReadEventData> events;
    private long nextEventNumber;
    private long lastEventNumber;
    private boolean isEndOfStream;
    private boolean streamExists = false;

    @Builder(builderMethodName = "create")
    public StreamReadResults(String streamName, long fromEventNumber, Direction direction, List<ReadEventData> events, long nextEventNumber, long lastEventNumber, boolean isEndOfStream) {

        if (StringUtils.isEmpty(streamName)) { throw new IllegalArgumentException("Stream cannot be null, empty or whitespace"); }

        this.streamName = streamName;
        this.fromEventNumber = fromEventNumber;
        this.direction = direction;
        this.events = Optional.ofNullable(events).orElse(new ArrayList<>());
        this.nextEventNumber = nextEventNumber;
        this.lastEventNumber = lastEventNumber;
        this.isEndOfStream = isEndOfStream;
        this.streamExists = true;
    }

    private StreamReadResults() {}

    public static StreamReadResults noStream() {
        return new StreamReadResults();
    }

    public boolean isEmpty() {
        return CollectionUtils.isEmpty(events);
    }

    public static StreamReadResults empty() {
        return new StreamReadResults();
    }

}
