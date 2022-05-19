package com.dewdrop.streamstore.eventstore;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import com.dewdrop.structure.events.WriteEventData;
import com.dewdrop.structure.read.ReadRequest;
import com.dewdrop.structure.events.StreamReadResults;
import com.dewdrop.structure.events.ReadEventData;
import com.dewdrop.structure.read.Direction;
import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.ReadResult;
import com.eventstore.dbclient.ReadStreamOptions;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.dbclient.StreamRevision;
import com.eventstore.dbclient.Subscription;
import com.eventstore.dbclient.SubscriptionListener;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class EventStoreUtils {
    public static StreamReadResults toStreamReadResults(ReadRequest readRequest, ReadResult readResult) {
        requireNonNull(readResult);

        List<ResolvedEvent> events = readResult.getEvents();
        List<ReadEventData> recordedEvents = events.stream().map(ResolvedEvent::getEvent).map(EventStoreUtils::toReadEventData).collect(toList());

        Long currentRevision = recordedEvents.stream().mapToLong(event -> {
            return event.getEventNumber();
        }).max().orElse(0L);
        // TODO: How do we calculate next and last event numbers?
        return new StreamReadResults(readRequest.getStream(), readRequest.getStart(), readRequest.getDirection(), recordedEvents, currentRevision, currentRevision, true);
    }


    public static ReadEventData toReadEventData(RecordedEvent recordedEvent) {
        return new ReadEventData(recordedEvent.getStreamId(), UUID.fromString(recordedEvent.getEventId().toString()), recordedEvent.getStreamRevision().getValueUnsigned(), recordedEvent.getEventType(), recordedEvent.getEventData(),
                        recordedEvent.getUserMetadata(), true, recordedEvent.getCreated());
    }


    public static ReadStreamOptions options(ReadRequest readRequest) {
        ReadStreamOptions readStreamOptions = ReadStreamOptions.get();
        StreamRevision streamRevision = readRequest.getStart() == null ? StreamRevision.START : new StreamRevision(readRequest.getStart());
        readStreamOptions.fromRevision(streamRevision);
        if (readRequest.getDirection() == Direction.FORWARD) {
            readStreamOptions.forwards();
        } else {
            readStreamOptions.backwards();
        }

        readStreamOptions.resolveLinkTos(true);
        return readStreamOptions;
    }

    public static SubscriptionListener createListener(Consumer<ReadEventData> eventAppeared) {
        return new SubscriptionListener() {
            @Override
            public void onEvent(Subscription subscription, ResolvedEvent event) {
                log.info("Received event:{}", event.getEvent().getEventType());
                try {
                    eventAppeared.accept(EventStoreUtils.toReadEventData(event.getEvent()));
                } catch (Exception e) {
                    log.error("Unable to accept event:{}", event.getEvent().getEventType(), e);
                }

            }

            @Override
            public void onError(Subscription subscription, Throwable throwable) {
                log.error("There was an error receiving the event? the subscription id:{}", subscription.getSubscriptionId(), throwable.getCause().getMessage());
            }

            @Override
            public void onCancelled(Subscription subscription) {
                log.error("Cancelling subscription id:" + subscription.getSubscriptionId());
            }
        };
    }

    public static EventData toEventData(WriteEventData eventData) {
        return new EventData(eventData.getEventId(), eventData.getEventType(), eventData.getEventType(), eventData.getData(), eventData.getMetadata());
    }
}
