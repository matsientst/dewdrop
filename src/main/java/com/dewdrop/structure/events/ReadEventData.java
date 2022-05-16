package com.dewdrop.structure.events;

import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Data
public class ReadEventData {
    protected UUID eventId;
    protected String eventType;
    protected boolean isJson;
    protected byte[] data;
    protected byte[] metadata;
    private final String eventStreamId;
    private final long eventNumber;
    private final Instant created;
    private final long createdEpoch;

    public ReadEventData(String eventStreamId, UUID eventId, long eventNumber, String eventType, byte[] data, byte[] metadata, boolean isJson, Instant created) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.isJson = isJson;
        this.data = data;
        this.metadata = metadata;
        this.eventStreamId = eventStreamId;
        this.eventNumber = eventNumber;
        this.created = created;
        this.createdEpoch = created.toEpochMilli();
    }
}
