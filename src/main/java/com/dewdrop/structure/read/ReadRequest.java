package com.dewdrop.structure.read;

import com.dewdrop.read.StreamDetails;
import java.util.Optional;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ReadRequest {
    String streamName;
    Long start;
    Long count;
    Direction direction;

    public ReadRequest(String streamName, Long start, Long count, Direction direction) {
        this.streamName = streamName;
        this.count = Optional.ofNullable(count)
            .orElse(Long.MAX_VALUE);
        this.start = start;
        this.direction = direction;
    }

    public static ReadRequest from(StreamDetails streamDetails, Long start, Long count) {
        return new ReadRequest(streamDetails.getStreamName(), start, count, streamDetails.getDirection());
    }
}
