package events.dewdrop.read.readmodel.stream;

import events.dewdrop.aggregate.AggregateRoot;
import events.dewdrop.read.readmodel.stream.subscription.Subscription;
import events.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import events.dewdrop.streamstore.write.StreamWriter;
import events.dewdrop.structure.api.Event;
import events.dewdrop.structure.datastore.StreamStore;
import events.dewdrop.structure.read.Handler;
import events.dewdrop.structure.serialize.EventSerializer;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

@Data
@Log4j2
public class Stream<T extends Event> implements Handler<T> {
    private Subscription<T> subscription;
    StreamStore streamStore;
    EventSerializer eventSerializer;
    StreamDetails streamDetails;
    private AtomicLong streamPosition;
    private final ScheduledExecutorService executorService;

    public Stream(StreamDetails streamDetails, StreamStore streamStore, EventSerializer eventSerializer) {
        requireNonNull(streamDetails, "StreamDetails needed for a valid stream");
        requireNonNull(streamStore, "StreamStore needed for a valid stream");
        requireNonNull(eventSerializer, "EventSerializer needed for a valid stream");

        this.streamDetails = streamDetails;
        this.streamStore = streamStore;
        this.eventSerializer = eventSerializer;
        this.streamPosition = new AtomicLong(0L);
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void subscribe() {
        if (!streamDetails.isSubscribed()) { return; }
        log.debug("Creating Subscription for:{} - direction: {}, type: {}, messageType:{}", streamDetails.getStreamName(), streamDetails.getDirection(), streamDetails.getStreamType(),
                        streamDetails.getMessageTypes().stream().map(event -> event.getClass().getSimpleName()).collect(joining(",")));
        subscription = Subscription.getInstance(this);
        StreamReader streamReader = StreamReader.getInstance(streamStore, eventSerializer, streamDetails);

        if (!subscription.subscribeByNameAndPosition(streamReader)) {
            log.info("Unable to find stream:{} will poll until we find then subscribe", streamDetails.getStreamName());
            pollForCompletion();
            return;
        }
    }

    /**
     * When the stream has not been found create a poll task to subscribe to the stream.
     */
    public void pollForCompletion() {
        StreamReader streamReader = StreamReader.getInstance(streamStore, eventSerializer, streamDetails);
        CompletableFuture<Boolean> completionFuture = new CompletableFuture<>();
        Runnable runnable = () -> {
            Boolean complete = subscription.subscribeByNameAndPosition(streamReader);
            if (complete) {
                log.info("Finally discovered stream: {}", streamReader.getStreamName());
                completionFuture.complete(complete);
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
    void schedule(StreamReader streamReader, CompletableFuture<Boolean> completionFuture, Runnable runnable) {
        final ScheduledFuture<?> checkFuture = executorService.scheduleAtFixedRate(runnable, 1, 5, TimeUnit.SECONDS);
        completionFuture.thenApply(result -> {
            subscription.subscribeByNameAndPosition(streamReader);
            return true;
        });
        completionFuture.whenComplete((result, thrown) -> checkFuture.cancel(false));
    }

    public void read(Long start, Long count) {
        StreamReader streamReader = StreamReader.getInstance(streamStore, eventSerializer, streamDetails, streamPosition);
        streamReader.read(start, count);
        this.streamPosition = streamReader.getStreamPosition();
    }

    public void read(StreamDetails idBasedDetails, Long start, Long count) {
        StreamReader streamReader = StreamReader.getInstance(streamStore, eventSerializer, idBasedDetails, streamPosition);
        streamReader.read(start, count);
        this.streamPosition = streamReader.getStreamPosition();
    }

    @Override
    public void handle(T event) {
        streamDetails.getEventHandler().accept(event);
    }

    // If we don't have a subscription we can call read to catch up to where we need to be in our
    // version
    public void updateQueryState(Optional<UUID> aggregateId) {
        if (aggregateId.isPresent()) {
            StreamDetails idDetails = StreamDetails.builder().streamType(streamDetails.getStreamType()).direction(streamDetails.getDirection()).eventHandler(streamDetails.getEventHandler()).aggregateName(streamDetails.getStreamName())
                            .streamNameGenerator(streamDetails.getStreamNameGenerator()).messageTypes(streamDetails.getMessageTypes()).name(streamDetails.getStreamName()).id(aggregateId.get()).subscribed(streamDetails.isSubscribed())
                            .startPositionMethod(streamDetails.getStartPositionMethod()).create();
            this.read(idDetails, this.streamPosition.get(), null);
        } else if (!streamDetails.isSubscribed()) {
            this.read(this.streamPosition.get(), null);
        }
    }

    public AggregateRoot getById(StreamStoreGetByIDRequest request) {
        requireNonNull(request, "A StreamStoreGetByIDRequest is required");

        if (streamDetails.getStreamType() != StreamType.AGGREGATE) { throw new IllegalStateException("Stream is not an aggregate - we cannot get by id"); }

        StreamReader streamReader = StreamReader.getInstance(streamStore, eventSerializer, streamDetails);
        return streamReader.getById(request);
    }

    public void save(AggregateRoot aggregateRoot) {
        StreamWriter streamWriter = StreamWriter.getInstance(streamDetails, streamStore, eventSerializer);
        streamWriter.save(aggregateRoot);
    }
}
