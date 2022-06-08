package com.dewdrop.streamstore.process;

import static java.util.Objects.requireNonNull;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.read.readmodel.StreamFactory;
import com.dewdrop.read.readmodel.stream.Stream;
import com.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import com.dewdrop.structure.api.Command;
import com.dewdrop.utils.AggregateIdUtils;
import com.dewdrop.utils.AggregateUtils;
import java.util.Optional;
import java.util.UUID;

public class StandaloneAggregateProcessor {
    StreamFactory streamFactory;

    public StandaloneAggregateProcessor(StreamFactory streamFactory) {
        this.streamFactory = streamFactory;
    }

    public AggregateRoot getById(Object aggregateObject, UUID id) {
        return getById(aggregateObject, id, null);
    }

    public AggregateRoot getById(Object aggregateObject, UUID id, Command command) {
        requireNonNull(aggregateObject, "aggregate is required");
        requireNonNull(id, "UUID is required");

        AggregateRoot aggregateRoot = getAggregateRoot(aggregateObject);

        Stream stream = streamFactory.constructStream(aggregateRoot, id);

        StreamStoreGetByIDRequest request = StreamStoreGetByIDRequest.builder().aggregateRoot(aggregateRoot).id(id).command(command).create();
        aggregateRoot = stream.getById(request);

        return aggregateRoot;
    }

    AggregateRoot getAggregateRoot(Object aggregateObject) {
        AggregateRoot aggregateRoot;
        if (aggregateObject instanceof AggregateRoot) {
            aggregateRoot = (AggregateRoot) aggregateObject;
        } else {
            aggregateRoot = AggregateUtils.create(aggregateObject.getClass()).orElse(null);
        }
        return aggregateRoot;
    }

    public AggregateRoot save(AggregateRoot aggregateRoot) {
        Optional<UUID> aggregateId = AggregateIdUtils.getAggregateId(aggregateRoot.getTarget());
        if (aggregateId.isEmpty()) { throw new IllegalArgumentException("Aggregate ID is not set"); }
        streamFactory.constructStream(aggregateRoot, aggregateId.get()).save(aggregateRoot);
        return aggregateRoot;
    }
}
