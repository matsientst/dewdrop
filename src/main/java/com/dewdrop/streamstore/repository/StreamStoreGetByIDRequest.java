package com.dewdrop.streamstore.repository;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.read.StreamDetails;
import com.dewdrop.structure.api.Command;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
public class StreamStoreGetByIDRequest {
    private String streamName;
    private AggregateRoot aggregateRoot;
    private UUID id;
    private int version;
    private Command command;

    @Builder(buildMethodName = "create")
    public StreamStoreGetByIDRequest(StreamDetails streamDetails, AggregateRoot aggregateRoot, UUID id, Integer version, Command command) {
        this.streamName = streamDetails.getStreamName();
        this.aggregateRoot = aggregateRoot;
        this.id = id;
        this.version = Optional.ofNullable(version)
            .orElse(Integer.MAX_VALUE);
        this.command = command;
    }
}
