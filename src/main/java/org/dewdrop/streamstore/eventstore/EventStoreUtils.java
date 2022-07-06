package org.dewdrop.streamstore.eventstore;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import org.dewdrop.structure.events.ReadEventData;
import org.dewdrop.structure.events.StreamReadResults;
import org.dewdrop.structure.events.WriteEventData;
import org.dewdrop.structure.read.Direction;
import org.dewdrop.structure.read.ReadRequest;
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

        Long currentRevision = events.stream().map(ResolvedEvent::getLink).mapToLong(link -> {
            if (link == null) { return 0L; }
            return link.getStreamRevision().getValueUnsigned();
        }).max().orElse(0L);

        boolean isEndOfStream = readResult.getEvents().isEmpty() || readResult.getEvents().size() < readRequest.getCount();
        return new StreamReadResults(readRequest.getStreamName(), readRequest.getStart(), readRequest.getDirection(), recordedEvents, currentRevision + 1, currentRevision, isEndOfStream);
    }

    public static ReadEventData toReadEventData(RecordedEvent recordedEvent) {
        return new ReadEventData(recordedEvent.getStreamId(), UUID.fromString(recordedEvent.getEventId().toString()), recordedEvent.getStreamRevision().getValueUnsigned(), recordedEvent.getEventType(), recordedEvent.getEventData(),
                        recordedEvent.getUserMetadata(), true, recordedEvent.getCreated());
    }

    public static ReadEventData toReadEventData(ResolvedEvent resolvedEvent) {
        RecordedEvent link = resolvedEvent.getLink();
        RecordedEvent event = resolvedEvent.getEvent();
        return new ReadEventData(link.getStreamId(), UUID.fromString(link.getEventId().toString()), link.getStreamRevision().getValueUnsigned(), event.getEventType(), event.getEventData(), event.getUserMetadata(), true, event.getCreated());
    }


    public static ReadStreamOptions options(ReadRequest readRequest) {
        ReadStreamOptions readStreamOptions = ReadStreamOptions.get();

        readStreamOptions.resolveLinkTos();
        if (readRequest.getDirection() == Direction.FORWARD) {
            StreamRevision streamRevision = readRequest.getStart() == null ? StreamRevision.START : new StreamRevision(readRequest.getStart());
            readStreamOptions.fromRevision(streamRevision);
            readStreamOptions.forwards();
        } else {
            readStreamOptions.backwards();
            readStreamOptions.fromRevision(StreamRevision.END);
        }

        readStreamOptions.resolveLinkTos(true);
        return readStreamOptions;
    }

    public static SubscriptionListener createListener(Consumer<ReadEventData> eventAppeared) {
        return new SubscriptionListener() {
            @Override
            public void onEvent(Subscription subscription, ResolvedEvent event) {
                RecordedEvent recordedEvent = event.getLink();
                String eventType = event.getEvent().getEventType();
                log.debug("Received event:{}, from stream:{}, position:{}", eventType, recordedEvent.getStreamId(), recordedEvent.getStreamRevision().getValueUnsigned());
                try {
                    eventAppeared.accept(EventStoreUtils.toReadEventData(event));
                } catch (Exception e) {
                    log.error("Unable to accept event:{}", recordedEvent.getEventType(), e);
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