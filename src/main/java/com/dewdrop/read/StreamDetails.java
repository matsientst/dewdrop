package com.dewdrop.read;

import com.dewdrop.streamstore.stream.PrefixStreamNameGenerator;
import com.dewdrop.structure.StreamNameGenerator;
import lombok.Data;

@Data
public class StreamDetails {
    private StreamType streamType;
    private String name;
    private String streamName;
    StreamNameGenerator streamNameGenerator = new PrefixStreamNameGenerator();

    public StreamDetails(StreamType streamType, String name) {
        this.streamType = streamType;
        this.name = name;
        this.streamName = streamType == StreamType.CATEGORY ? streamNameGenerator.generateForCategory(name) : streamNameGenerator.generateForEvent(name);
    }
}
