package com.dewdrop.read.readmodel.stream;

import static java.util.Objects.requireNonNull;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class NameAndPosition {
    private String streamName;
    private Long position;

    private StreamType streamType;
    private String name;

    private NameAndPosition() {}

    @Builder(buildMethodName = "create")
    public NameAndPosition(StreamType streamType, String name) {
        requireNonNull(streamType, "streamType is required");
        requireNonNull(name, "name is required");

        this.streamType = streamType;
        this.name = name;
    }

    public boolean isComplete() {
        return StringUtils.isNotEmpty(streamName);
    }

    public NameAndPosition completeTask(String streamName, Long position) {
        this.streamName = streamName;
        this.position = position;
        return this;
    }
}
