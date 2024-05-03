package events.dewdrop.read.readmodel.stream.subscription;

import static java.util.Objects.requireNonNull;

import events.dewdrop.read.readmodel.stream.NameAndPosition;
import events.dewdrop.read.readmodel.stream.SubscriptionStartStrategy;
import events.dewdrop.structure.subscribe.EventProcessor;
import events.dewdrop.read.readmodel.stream.Stream;
import events.dewdrop.read.readmodel.stream.StreamListener;
import events.dewdrop.read.readmodel.stream.StreamReader;
import events.dewdrop.structure.api.Event;
import events.dewdrop.structure.read.Handler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class Subscription<T extends Event> {
    private final Map<Class<?>, List<EventProcessor<T>>> handlers = new ConcurrentHashMap<>();
    protected final StreamListener<T> listener;
    private final List<Class<? extends Event>> messageTypes;
    private final Handler<T> handler;
    private final ScheduledExecutorService executorService;

    Subscription(Handler<T> handler, List<Class<? extends Event>> messageTypes, StreamListener<T> listener) {
        this.messageTypes = messageTypes;
        this.handler = handler;
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.listener = listener;
        registerHandlers();
    }

    public static Subscription getInstance(Stream stream) {
        return new Subscription<>(stream, stream.getStreamDetails().getMessageTypes(), StreamListener.getInstance(stream.getStreamStore(), stream.getEventSerializer()));
    }

    void registerToMessageType(EventProcessor<T> eventProcessor, Class<?> eventType) {
        List<EventProcessor<T>> handlesFor = getHandlesFor(eventType);
        boolean isSame = handlesFor.stream().anyMatch(handle -> handle.isSame(eventType, eventProcessor));
        if (!isSame) {
            synchronized (this.handlers) {
                this.handlers.computeIfAbsent(eventType, item -> new ArrayList<>());
                this.handlers.get(eventType).add(eventProcessor);
            }
        }
    }

    List<EventProcessor<T>> getHandlesFor(Class<?> type) {
        requireNonNull(type, "Type is required");

        synchronized (this.handlers) {
            if (this.handlers.containsKey(type)) { return new ArrayList<>(this.handlers.get(type)); }
            return new ArrayList<>();
        }
    }

    public void registerHandlers() {
        EventProcessor<T> allHandler = new EventProcessor<>(handler, getMessageTypes());

        for (Class<?> currentMessageType : getMessageTypes()) {
            registerToMessageType(allHandler, currentMessageType);
        }
    }

    public void publish(T event) {
        requireNonNull(event, "event is required");

        log.debug("Publishing event:{}, handlers: {}", event.getClass().getSimpleName(), this.handlers.size());
        // Call each handler registered to the event type.
        List<EventProcessor<T>> eventProcessors = getHandlesFor(event.getClass());

        eventProcessors.forEach(handle -> handle.process(event));
    }

    public boolean subscribeByNameAndPosition(StreamReader streamReader) {
        NameAndPosition nameAndPosition = streamReader.nameAndPosition();
        if (!streamReader.isStreamExists()) { return false; }
        boolean subscribed = listener.start(nameAndPosition.getStreamName(), nameAndPosition.getPosition(), this);
        if (subscribed) {
            log.info("Completed subscription to stream: {} from position:{}", nameAndPosition.getStreamName(), nameAndPosition.getPosition());
        }
        return subscribed;
    }

    /**
     * When the stream has not been found create a poll task to subscribe to the stream.
     *
     * @param streamReader - A constructed streamReader to read from the stream
     */
    public void pollForCompletion(StreamReader streamReader) {
        CompletableFuture<NameAndPosition> completionFuture = new CompletableFuture<>();
        Runnable runnable = () -> {
            NameAndPosition result = streamReader.nameAndPosition();
            if (result.isComplete()) {
                log.info("Finally discovered stream: {}", result.getStreamName());
                completionFuture.complete(result);
            }
            if (!streamReader.isStreamExists()) {
                log.info("Stream: {} still not found", streamReader.getStreamName());
            }
        };
        schedule(streamReader, completionFuture, runnable);
    }

    /**
     * Schedule the lookup for the stream name and position. When found automatically subscribe.
     *
     * @param streamReader
     * @param completionFuture
     * @param runnable
     */
    void schedule(StreamReader streamReader, CompletableFuture<NameAndPosition> completionFuture, Runnable runnable) {
        final ScheduledFuture<?> checkFuture = executorService.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.SECONDS);
        completionFuture.thenApply(result -> {
            subscribeByNameAndPosition(streamReader);
            return true;
        });
        completionFuture.whenComplete((result, thrown) -> checkFuture.cancel(false));
    }

}
