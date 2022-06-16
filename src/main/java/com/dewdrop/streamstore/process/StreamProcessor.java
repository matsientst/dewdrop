package com.dewdrop.streamstore.process;

import static java.util.Objects.requireNonNull;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.api.result.Result;
import com.dewdrop.read.readmodel.StreamFactory;
import com.dewdrop.read.readmodel.stream.Stream;
import com.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import com.dewdrop.structure.api.Command;
import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.api.Message;
import com.dewdrop.utils.CommandHandlerUtils;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class StreamProcessor {
    StreamFactory streamFactory;

    public StreamProcessor(StreamFactory streamFactory) {
        requireNonNull(streamFactory, "StreamFactory is required");

        this.streamFactory = streamFactory;
    }


    public <T, R extends Event> Result<T> process(Command command, Method commandHandlerMethod, AggregateRoot aggregateRoot, UUID aggregateRootId) {
        requireNonNull(command, "command is required");
        requireNonNull(commandHandlerMethod, "commandHandlerMethod is required");
        requireNonNull(aggregateRoot, "aggregateRoot is required");
        requireNonNull(aggregateRootId, "aggregateRootId is required");

        log.debug("Processing command {}", command.getClass().getSimpleName());
        Stream<R> stream = streamFactory.constructStream(aggregateRoot, aggregateRootId);
        aggregateRoot = getById(stream, command, aggregateRoot, aggregateRootId);
        aggregateRoot = executeCommand(command, commandHandlerMethod, aggregateRoot);
        aggregateRoot = save(stream, aggregateRoot);
        return (Result<T>) Result.of(aggregateRoot.getTarget());
    }


    protected AggregateRoot getById(Stream stream, Command command, AggregateRoot aggregateRoot, UUID aggregateRootId) {
        StreamStoreGetByIDRequest request = StreamStoreGetByIDRequest.builder().aggregateRoot(aggregateRoot).id(aggregateRootId).command(command).create();
        return stream.getById(request);
    }

    protected <T> AggregateRoot executeCommand(Command command, Method commandHandlerMethod, AggregateRoot aggregateRoot) {
        log.debug("executing command:{} for aggregateRoot:{}", command.getClass().getSimpleName(), aggregateRoot.getTargetClassName());
        Optional<T> result = CommandHandlerUtils.executeCommand(commandHandlerMethod, command, aggregateRoot);
        result.ifPresent(events -> processEvents(aggregateRoot, events));
        return aggregateRoot;
    }

    <T> void processEvents(AggregateRoot aggregateRoot, T events) {
        if (events instanceof List) {
            for (Event event : (List<Event>) events) {
                aggregateRoot.raise(event);
            }
        } else {
            aggregateRoot.raise((Event) events);
        }
    }

    protected AggregateRoot save(Stream stream, AggregateRoot aggregateRoot) {
        stream.save(aggregateRoot);
        return aggregateRoot;
    }
}
