package com.dewdrop.streamstore.eventstore;

import com.dewdrop.streamstore.serialize.JsonSerializer;
import com.eventstore.dbclient.Position;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.dbclient.StreamRevision;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.Builder;

public class EventStoreTestUtils {
    private static final String eventStreamId = UUID.randomUUID().toString();
    private static final StreamRevision streamRevision = new StreamRevision(0L);
    private static final UUID eventId = UUID.randomUUID();
    private static final String eventType = "eventType";
    private static byte[] eventData;
    private static byte[] userMetadata;
    private static final Instant created = Instant.now();
    private static final String contentType = "contentType";
    private static Map<String, String> systemMetadata;


    public EventStoreTestUtils(Map<String, String> eventData, Map<String, String> userMetadata, Map<String, String> systemMetadata) {
        EventStoreTestUtils.eventData = createByteData(eventData);
        EventStoreTestUtils.userMetadata = createByteData(userMetadata);
        EventStoreTestUtils.systemMetadata = systemMetadata;
    }

    public static Map<String, String> createSystemMetaData(String eventType, String contentType, Instant created) {
        return Map.of("type", eventType, "content-type", contentType, "created", created.toEpochMilli() + "");
    };

    @Builder(builderMethodName = "buildRecordedEvent")
    public static RecordedEvent createRecordedEvent(Position position) {
        return new RecordedEvent(eventStreamId, streamRevision, eventId, position, systemMetadata, eventData, userMetadata);
    };

    public static RecordedEvent createRecordedEvent() {
        return createRecordedEvent(new Position(1L, 0L));
    }

    @Builder(builderMethodName = "buildResolvedEvent")
    public static ResolvedEvent createResolvedEvent(@NotNull RecordedEvent recordedEvent, Position position) {
        return new ResolvedEvent(recordedEvent, createRecordedEvent(position));
    }

    public static ResolvedEvent createResolvedEvent() {
        RecordedEvent recordedEvent = createRecordedEvent();
        Position position = new Position(2L, 1L);
        return new ResolvedEvent(recordedEvent, createRecordedEvent(position));
    }

    public static byte[] createByteData(Map byteData) {
        JsonSerializer serializer = new JsonSerializer(new ObjectMapper());
        return serializer.serialize(byteData).get().getData();
    }

}
