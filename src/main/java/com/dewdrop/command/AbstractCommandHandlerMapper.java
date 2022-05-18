package com.dewdrop.command;

import static java.util.Objects.requireNonNull;

import com.dewdrop.streamstore.repository.StreamStoreRepository;
import com.dewdrop.structure.api.Command;

public abstract class AbstractCommandHandlerMapper implements CommandMapper {
    protected StreamStoreRepository streamStoreRepository;

    public void construct(StreamStoreRepository streamStoreRepository) {
        requireNonNull(streamStoreRepository, "StreamStoreRepository is required");

        this.streamStoreRepository = streamStoreRepository;
    }

}
