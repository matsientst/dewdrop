package com.dewdrop.streamstore.subscribe;

import static com.dewdrop.streamstore.subscribe.EventClassHierarchy.getMyChildren;
import static java.util.Objects.requireNonNull;

import com.dewdrop.read.NameAndPosition;
import com.dewdrop.read.StreamDetails;
import com.dewdrop.read.StreamReader;
import com.dewdrop.structure.NoStreamException;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.read.Handler;
import com.dewdrop.structure.serialize.EventSerializer;
import com.dewdrop.structure.subscribe.EventHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class Subscription<T extends Message> {
    private final Map<Class<?>, List<EventHandler<T>>> handlers = new ConcurrentHashMap<>();
    protected final StreamListener<T> listener;
    private final StreamStore streamStoreConnection;
    private final EventSerializer eventSerializer;
    private final Class<?> eventType;
    private final Handler<T> handler;
    private final ScheduledExecutorService executorService;

    public Subscription(Handler<T> handler, Class<?> eventType, StreamStore streamStoreConnection, EventSerializer eventSerializer) {
        this.streamStoreConnection = streamStoreConnection;
        this.eventSerializer = eventSerializer;
        this.eventType = eventType;
        this.handler = handler;
        this.executorService = Executors.newScheduledThreadPool(2);
        this.listener = new StreamListener<>(eventType, streamStoreConnection, eventSerializer);
        registerHandlers(handler);
    }


    public AutoCloseable subscribe(Handler<T> handler, boolean includeDerived) {
        requireNonNull(handler, "handler");

        EventHandler<T> tEventHandler = new EventHandler<>(handler, handler.getClass()
            .getSimpleName());
        registerHandler(tEventHandler, includeDerived);

        return () -> unsubscribe(handler);
    }

    void registerHandler(EventHandler<T> handler, boolean includeDerived) {
        List<Class<?>> eventTypes;
        if (includeDerived) {
            eventTypes = new ArrayList<>(getMyChildren(handler.getMessageType()));
        } else {
            eventTypes = List.of(handler.getMessageType());

        }
        for (Class<?> currentMessageType : eventTypes) {
            registerToMessageType(handler, currentMessageType);
        }
    }

    void registerToMessageType(EventHandler<T> handler, Class<?> eventType) {
        List<EventHandler<T>> handlesFor = getHandlesFor(eventType);
        boolean isSame = handlesFor.stream()
            .anyMatch(handle -> handle.isSame(eventType, handler));
        if (!isSame) {
            synchronized (this.handlers) {
                this.handlers.computeIfAbsent(eventType, item -> new ArrayList<>());
                this.handlers.get(eventType)
                    .add(handler);
            }
        }
    }

    public void unsubscribe(Handler<T> handler) {
        requireNonNull(handler, "handler");

        Set<Class<?>> descendants = getMyChildren(handler.getMessageType());
        for (Class<?> clazz : descendants) {
            List<EventHandler<T>> handlesFor = getHandlesFor(clazz);

            new ArrayList<>(handlesFor).forEach(EventHandler -> {
                if (EventHandler.isSame(handler.getMessageType(), handler)) {
                    this.handlers.get(clazz)
                        .remove(EventHandler);
                }
            });
        }
    }

    List<EventHandler<T>> getHandlesFor(Class<?> type) {
        requireNonNull(type, "Type is required");

        synchronized (this.handlers) {
            if (this.handlers.containsKey(type)) {return new ArrayList<>(this.handlers.get(type));}
            return new ArrayList<>();
        }
    }

    public AutoCloseable registerHandlers(Handler<T> handler) {
        requireNonNull(handler, "handler is required");

        EventHandler<T> allHandler = new EventHandler<>(handler, handler.getClass()
            .getSimpleName());
        // THis is BAD? Why traverse all objects?
        Set<Class<?>> eventTypes = EventClassHierarchy.getMyChildren(allHandler.getMessageType());

        for (Class<?> currentMessageType : eventTypes) {
            registerToMessageType(allHandler, currentMessageType);
        }

        return () -> unsubscribe(handler);
    }

    public void publish(T event) {
        requireNonNull(event, "event is required");

        log.info("Publishing event:{}, handlers: {}", event.getClass()
            .getSimpleName(), this.handlers.size());
        // Call each handler registered to the event type.
        List<EventHandler<T>> eventHandlers = getHandlesFor(event.getClass());

        eventHandlers.forEach(handle -> handle.tryHandle(event));
    }

    public void readAndSubscribe(StreamDetails streamDetails) {
        StreamReader streamReader = new StreamReader(streamStoreConnection, eventSerializer, streamDetails);

        try {
            subscribeByNameAndPosition(streamReader);
        } catch (NoStreamException e) {
            pollForCompletion(streamReader);
        }
    }

    public void subscribeByNameAndPosition(StreamReader streamReader) {
        NameAndPosition nameAndPosition = streamReader.getNameAndPosition();
        try {
            listener.start(nameAndPosition.getStreamName(), nameAndPosition.getPosition(), this);
            log.info("Completed subscription to stream: {} from position:{}", nameAndPosition.getStreamName(), nameAndPosition.getPosition());
        } catch (Exception e) {
            log.error("Problem starting subscription: {} at: {}", nameAndPosition.getStreamName(), nameAndPosition.getPosition(), e);
        }
    }

    public void pollForCompletion(StreamReader streamReader) {
        NameAndPosition nameAndPosition = streamReader.getNameAndPosition();
        CompletableFuture<NameAndPosition> completionFuture = new CompletableFuture<>();
        Runnable runnable = () -> {
            NameAndPosition result;
            try {
                result = streamReader.getNameAndPosition();
                if (result.isComplete()) {
                    log.info("Found stream: {}", result.getStreamName());
                    completionFuture.complete(result);
                }
            } catch (NoStreamException e) {
                log.debug("Stream: {} not found", nameAndPosition.getStreamName());
            }
        };
        schedule(streamReader, completionFuture, runnable);
    }

    void schedule(StreamReader streamReader, CompletableFuture<NameAndPosition> completionFuture, Runnable runnable) {
        final ScheduledFuture<?> checkFuture = executorService.scheduleAtFixedRate(runnable, 0, 3, TimeUnit.SECONDS);
        completionFuture.thenApply(result -> {
            log.info("adding subscription");
            subscribeByNameAndPosition(streamReader);
            return true;
        });
        completionFuture.whenComplete((result, thrown) -> checkFuture.cancel(false));
    }
}