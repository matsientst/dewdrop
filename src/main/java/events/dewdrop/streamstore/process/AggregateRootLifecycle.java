package events.dewdrop.streamstore.process;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

import events.dewdrop.aggregate.AggregateRoot;
import events.dewdrop.api.result.Result;
import events.dewdrop.api.validators.ValidationException;
import events.dewdrop.read.readmodel.stream.Stream;
import events.dewdrop.read.readmodel.stream.StreamFactory;
import events.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import events.dewdrop.structure.api.Command;
import events.dewdrop.structure.api.Event;
import events.dewdrop.structure.api.validator.DewdropValidator;
import events.dewdrop.utils.CommandHandlerUtils;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;


/**
 * The `AggregateRootLifecycle` class is a utility class that encapsulates the logic for handling
 * the lifecycle of an AggregateRoot
 */
@Log4j2
public class AggregateRootLifecycle {
    StreamFactory streamFactory;

    public AggregateRootLifecycle(StreamFactory streamFactory) {
        requireNonNull(streamFactory, "StreamFactory is required");

        this.streamFactory = streamFactory;
    }


    /**
     * Process a command by loading the aggregate root from the event stream, executing the command on
     * the aggregate root, and saving the aggregate root back to the event stream
     *
     * @param <T> The type of event that is handled by the stream
     * @param command The command to be processed
     * @param commandHandlerMethod The method that will be invoked on the aggregate root to process the
     *        command.
     * @param aggregateRoot The aggregate root that will be used to process the command.
     * @param aggregateRootId The id of the aggregate root
     * @return A {@code Result<Boolean>}
     * @throws ValidationException If the command is invalid
     */
    public <T extends Event> Result<Boolean> process(Command command, Method commandHandlerMethod, AggregateRoot aggregateRoot, UUID aggregateRootId) throws ValidationException {
        requireNonNull(command, "command is required");
        requireNonNull(commandHandlerMethod, "commandHandlerMethod is required");
        requireNonNull(aggregateRoot, "aggregateRoot is required");
        requireNonNull(aggregateRootId, "aggregateRootId is required");

        log.debug("Processing command {}", command.getClass().getSimpleName());
        Stream<T> stream = streamFactory.constructStreamFromAggregateRoot(aggregateRoot, aggregateRootId);
        validateCommand(command, commandHandlerMethod);
        aggregateRoot = getById(stream, command, aggregateRoot, aggregateRootId);
        aggregateRoot = executeCommand(command, commandHandlerMethod, aggregateRoot);
        save(stream, aggregateRoot);
        return Result.of(true);
    }

    protected void validateCommand(Command command, Method commandHandlerMethod) throws ValidationException {
        Parameter[] parameters = commandHandlerMethod.getParameters();
        if (parameters != null && parameters.length > 0) {
            boolean isValidate = parameters[0].isAnnotationPresent(Valid.class);
            boolean isCommand = ((Class<?>) parameters[0].getParameterizedType()) == command.getClass();
            if (isValidate && isCommand) {
                DewdropValidator.validate(command);
            }
        }
    }


    /**
     * "Get the aggregate root from the stream store by id."
     * <p>
     * The function takes the following parameters:
     * <p>
     * * stream - the stream store * command - the command that is being processed * aggregateRoot - the
     * aggregate root that is being processed * aggregateRootId - the id of the aggregate root that is
     * being processed
     * <p>
     * The function returns the aggregate root that was retrieved from the stream store
     *
     * @param stream The stream that the aggregate root is stored in.
     * @param command The command that is being executed.
     * @param aggregateRoot The aggregate root class
     * @param aggregateRootId The id of the aggregate root you want to get.
     * @return The aggregate root.
     */
    protected AggregateRoot getById(Stream stream, Command command, AggregateRoot aggregateRoot, UUID aggregateRootId) {
        StreamStoreGetByIDRequest request = StreamStoreGetByIDRequest.builder().aggregateRoot(aggregateRoot).id(aggregateRootId).command(command).create();
        return stream.getById(request);
    }

    /**
     * Execute the command handler method for the given command and aggregate root, and if the result is
     * an exception, throw it as a validation exception. Otherwise, process the events returned by the
     * command handler
     *
     * @param <T> The type of event that is returned by the command handler method
     * @param command The command to execute
     * @param commandHandlerMethod The method that will be invoked to execute the command.
     * @param aggregateRoot The aggregate root that the command is being executed on.
     * @return The aggregate root that was passed in.
     * @throws ValidationException If the command handler method throws a validation exception
     */
    protected <T> AggregateRoot executeCommand(Command command, Method commandHandlerMethod, AggregateRoot aggregateRoot) throws ValidationException {
        log.debug("executing command:{} for aggregateRoot:{}", command.getClass().getSimpleName(), aggregateRoot.getTargetClassName());
        aggregateRoot.setSource(command);
        Result<T> result = CommandHandlerUtils.executeCommand(commandHandlerMethod, command, aggregateRoot);
        if (result.isExceptionPresent()) { throw ValidationException.of(result.getException()); }
        result.ifPresent(events -> processEvents(aggregateRoot, events));
        return aggregateRoot;
    }

    /**
     * If the events are a list, then raise each event individually, otherwise raise the event
     *
     * @param <T> The type of event
     * @param aggregateRoot The aggregate root to apply the events to.
     * @param events The events to be processed.
     */
    <T> void processEvents(AggregateRoot aggregateRoot, T events) {
        if (events instanceof List) {
            for (Event event : (List<Event>) events) {
                aggregateRoot.raise(event);
            }
        } else {
            aggregateRoot.raise((Event) events);
        }
    }

    /**
     * Save the aggregate root to the stream and return it.
     *
     * @param stream The stream to save the aggregate root to.
     * @param aggregateRoot The aggregate root to be saved.
     * @return The aggregate root.
     */
    protected AggregateRoot save(Stream stream, AggregateRoot aggregateRoot) {
        stream.save(aggregateRoot);
        return aggregateRoot;
    }
}
