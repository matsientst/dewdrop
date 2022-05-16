package com.dewdrop.read;

import com.dewdrop.structure.api.Message;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NameAndPosition {
    private String streamName;
    private Long position;

    private StreamType streamType;
    private String name;
    private Consumer<Message> consumer;
    private Class<?> messageType;

    @Builder(buildMethodName = "create")
    public NameAndPosition(StreamType streamType, String name, Consumer<Message> consumer, Class<?> messageType) {
        this.streamType = streamType;
        this.name = name;
        this.consumer = consumer;
        this.messageType = messageType;
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
