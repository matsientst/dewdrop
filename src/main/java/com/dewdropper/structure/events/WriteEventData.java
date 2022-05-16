package com.dewdropper.structure.events;

import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class WriteEventData {
    protected UUID eventId;
    protected String eventType;
    protected boolean isJson;
    protected byte[] data;
    protected byte[] metadata;

    public WriteEventData(UUID eventId, String eventType, boolean isJson, byte[] data, byte[] metadata) {
        if (StringUtils.isEmpty(eventType)) throw new IllegalArgumentException("Type cannot be null, empty or whitespace");

        this.eventId = eventId;
        this.eventType = eventType;
        this.isJson = isJson;
        this.data = Optional.ofNullable(data).orElse(new byte[0]);
        this.metadata = Optional.ofNullable(metadata).orElse(new byte[0]);
    }
}
