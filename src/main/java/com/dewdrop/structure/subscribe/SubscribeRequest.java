package com.dewdrop.structure.subscribe;

import com.dewdrop.structure.events.ReadEventData;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.Data;

@Data
public class SubscribeRequest {
    String streamName;
    Long lastCheckpoint;
    Consumer<ReadEventData> consumeEvent;

    public SubscribeRequest(String streamName, Long lastCheckpoint, Consumer<ReadEventData> consumeEvent) {
        this.streamName = streamName;
        this.lastCheckpoint = Optional.ofNullable(lastCheckpoint).orElse(-1L);
        this.consumeEvent = consumeEvent;
    }
}
