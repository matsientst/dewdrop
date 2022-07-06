package org.dewdrop.streamstore.repository;

import org.dewdrop.aggregate.AggregateRoot;
import org.dewdrop.structure.api.Command;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
public class StreamStoreGetByIDRequest {
    private AggregateRoot aggregateRoot;
    private UUID id;
    private int version;
    private Command command;

    @Builder(buildMethodName = "create")
    public StreamStoreGetByIDRequest(AggregateRoot aggregateRoot, UUID id, Integer version, Command command) {
        this.aggregateRoot = aggregateRoot;
        this.id = id;
        this.version = Optional.ofNullable(version).orElse(Integer.MAX_VALUE);
        this.command = command;
    }
}
