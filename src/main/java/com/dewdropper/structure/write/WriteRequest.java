package com.dewdropper.structure.write;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.requireNonEmpty;

import com.dewdropper.structure.events.WriteEventData;
import com.eventstore.dbclient.UserCredentials;
import java.util.List;
import lombok.Data;

@Data
public class WriteRequest {
    String streamName;
    Long expectedVersion;
    List<WriteEventData> events;

    public WriteRequest(String streamName, Long expectedVersion, List<WriteEventData> events) {
        requireNonNull(streamName, "streamName is required");
        requireNonNull(events, "events is required");
        requireNonEmpty(events, "events is required");

        this.streamName = streamName;
        this.expectedVersion = expectedVersion;
        this.events = events;
    }
}
