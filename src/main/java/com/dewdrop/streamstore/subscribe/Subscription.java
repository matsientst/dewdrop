package com.dewdrop.streamstore.subscribe;

import static java.util.Objects.requireNonNull;

import com.dewdrop.read.NameAndPosition;
import com.dewdrop.read.StreamReader;
import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.read.Handler;
import com.dewdrop.structure.subscribe.EventProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final List<Class<?>> messageTypes;
    private final Handler<T> handler;
    private final ScheduledExecutorService executorService;

    public Subscription(Handler<T> handler, List<Class<?>> messageTypes, StreamListener<T> listener) {
        this.messageTypes = messageTypes;
        this.handler = handler;
        this.executorService = Executors.newScheduledThreadPool(2);
        this.listener = listener;
        registerHandlers();
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

    void schedule(StreamReader streamReader, CompletableFuture<NameAndPosition> completionFuture, Runnable runnable) {
        final ScheduledFuture<?> checkFuture = executorService.scheduleAtFixedRate(runnable, 0, 3, TimeUnit.SECONDS);
        completionFuture.thenApply(result -> {
            subscribeByNameAndPosition(streamReader);
            return true;
        });
        completionFuture.whenComplete((result, thrown) -> checkFuture.cancel(false));
    }
}
