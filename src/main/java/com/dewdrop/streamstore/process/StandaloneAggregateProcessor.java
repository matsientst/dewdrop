package com.dewdrop.streamstore.process;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.read.readmodel.StreamFactory;
import com.dewdrop.read.readmodel.stream.Stream;
import com.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import com.dewdrop.structure.api.Command;
import com.dewdrop.utils.AggregateIdUtils;
import com.dewdrop.utils.AggregateUtils;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;

public class StandaloneAggregateProcessor {
    StreamFactory streamFactory;

    @Builder(buildMethodName = "create")
    public StandaloneAggregateProcessor(StreamFactory streamFactory) {
        this.streamFactory = streamFactory;
    }

    public AggregateRoot getById(Object aggregateObject, UUID id) {
        return getById(aggregateObject, id, null);
    }

    public AggregateRoot getById(Object aggregateObject, UUID id, Command command) {
        AggregateRoot aggregateRoot;
        if (aggregateObject instanceof AggregateRoot) {
            aggregateRoot = (AggregateRoot) aggregateObject;
        } else {
            aggregateRoot = AggregateUtils.create(aggregateObject.getClass()).orElse(null);
        }

        Stream stream = streamFactory.constructStream(aggregateRoot, id);

        StreamStoreGetByIDRequest request = StreamStoreGetByIDRequest.builder().aggregateRoot(aggregateRoot).id(id).command(command).create();
        aggregateRoot = stream.getById(request);

        return aggregateRoot;
    }

    public AggregateRoot save(AggregateRoot aggregateRoot) {
        Optional<UUID> aggregateId = AggregateIdUtils.getAggregateId(aggregateRoot.getTarget());
        if (aggregateId.isEmpty()) { throw new IllegalArgumentException("Aggregate ID is not set"); }
        Stream stream = streamFactory.constructStream(aggregateRoot, aggregateId.get());
        stream.save(aggregateRoot);
        return aggregateRoot;
    }
}
