package org.dewdrop.streamstore.process;

import static java.util.Objects.requireNonNull;

import org.dewdrop.aggregate.AggregateRoot;
import org.dewdrop.api.result.Result;
import org.dewdrop.api.validators.ValidationException;
import org.dewdrop.read.readmodel.stream.StreamFactory;
import org.dewdrop.read.readmodel.stream.Stream;
import org.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import org.dewdrop.structure.api.Command;
import org.dewdrop.structure.api.Event;
import org.dewdrop.utils.CommandHandlerUtils;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class StreamProcessor {
    StreamFactory streamFactory;

    public StreamProcessor(StreamFactory streamFactory) {
        requireNonNull(streamFactory, "StreamFactory is required");

        this.streamFactory = streamFactory;
    }


    public <T extends Event> Result<Boolean> process(Command command, Method commandHandlerMethod, AggregateRoot aggregateRoot, UUID aggregateRootId) throws ValidationException {
        requireNonNull(command, "command is required");
        requireNonNull(commandHandlerMethod, "commandHandlerMethod is required");
        requireNonNull(aggregateRoot, "aggregateRoot is required");
        requireNonNull(aggregateRootId, "aggregateRootId is required");

        log.debug("Processing command {}", command.getClass().getSimpleName());
        Stream<T> stream = streamFactory.constructStreamFromAggregateRoot(aggregateRoot, aggregateRootId);
        aggregateRoot = getById(stream, command, aggregateRoot, aggregateRootId);
        aggregateRoot = executeCommand(command, commandHandlerMethod, aggregateRoot);
        save(stream, aggregateRoot);
        return Result.of(true);
    }


    protected AggregateRoot getById(Stream stream, Command command, AggregateRoot aggregateRoot, UUID aggregateRootId) {
        StreamStoreGetByIDRequest request = StreamStoreGetByIDRequest.builder().aggregateRoot(aggregateRoot).id(aggregateRootId).command(command).create();
        return stream.getById(request);
    }

    protected <T> AggregateRoot executeCommand(Command command, Method commandHandlerMethod, AggregateRoot aggregateRoot) throws ValidationException {
        log.debug("executing command:{} for aggregateRoot:{}", command.getClass().getSimpleName(), aggregateRoot.getTargetClassName());
        Result<T> result = CommandHandlerUtils.executeCommand(commandHandlerMethod, command, aggregateRoot);
        if (result.isExceptionPresent()) { throw ValidationException.of(result.getException()); }
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
