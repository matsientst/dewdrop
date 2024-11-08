package events.dewdrop.streamstore.eventstore;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventDataBuilder;
import com.eventstore.dbclient.ReadResult;
import com.eventstore.dbclient.ReadStreamOptions;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.dbclient.StreamPosition;
import com.eventstore.dbclient.Subscription;
import com.eventstore.dbclient.SubscriptionListener;
import events.dewdrop.structure.events.ReadEventData;
import events.dewdrop.structure.events.StreamReadResults;
import events.dewdrop.structure.events.WriteEventData;
import events.dewdrop.structure.read.Direction;
import events.dewdrop.structure.read.ReadRequest;
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
            return link.getRevision();
        }).max().orElse(0L);

        boolean isEndOfStream = readResult.getEvents().isEmpty() || readResult.getEvents().size() < readRequest.getCount();
        return new StreamReadResults(readRequest.getStreamName(), readRequest.getStart(), readRequest.getDirection(), recordedEvents, currentRevision + 1, currentRevision, isEndOfStream);
    }

    public static ReadEventData toReadEventData(RecordedEvent recordedEvent) {
        return new ReadEventData(recordedEvent.getStreamId(), UUID.fromString(recordedEvent.getEventId().toString()), recordedEvent.getRevision(), recordedEvent.getEventType(), recordedEvent.getEventData(), recordedEvent.getUserMetadata(), true,
                        recordedEvent.getCreated());
    }

    public static ReadEventData toReadEventData(ResolvedEvent resolvedEvent) {
        RecordedEvent link = resolvedEvent.getLink();
        RecordedEvent event = resolvedEvent.getEvent();
        return new ReadEventData(link.getStreamId(), UUID.fromString(link.getEventId().toString()), link.getRevision(), event.getEventType(), event.getEventData(), event.getUserMetadata(), true, event.getCreated());
    }


    public static ReadStreamOptions options(ReadRequest readRequest) {
        ReadStreamOptions readStreamOptions = ReadStreamOptions.get();

        readStreamOptions.resolveLinkTos();
        if (readRequest.getDirection() == Direction.FORWARD) {
            StreamPosition streamRevision = readRequest.getStart() == null ? StreamPosition.start() : StreamPosition.position(readRequest.getStart());
            readStreamOptions.fromRevision(streamRevision);
            readStreamOptions.forwards();
        } else {
            readStreamOptions.backwards();
            readStreamOptions.fromRevision(StreamPosition.end());
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
                log.debug("Received event:{}, from stream:{}, position:{}", eventType, recordedEvent.getStreamId(), recordedEvent.getRevision());
                try {
                    eventAppeared.accept(EventStoreUtils.toReadEventData(event));
                } catch (Exception e) {
                    log.error("Unable to accept event:{}", recordedEvent.getEventType(), e);
                }

            }

            @Override
            public void onCancelled(Subscription subscription, Throwable exception) {
                log.error("Cancelling subscription id:" + subscription.getSubscriptionId());
            }
        };
    }

    public static EventData toEventData(WriteEventData eventData) {
        EventDataBuilder eventDataBuilder = EventDataBuilder.json(eventData.getEventId(), eventData.getEventType(), eventData.getData()).metadataAsBytes(eventData.getMetadata());
        return eventDataBuilder.build();
    }
}
