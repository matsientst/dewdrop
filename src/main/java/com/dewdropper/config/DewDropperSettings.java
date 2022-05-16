package com.dewdropper.config;

import com.dewdropper.DewDropper;
import com.dewdropper.aggregate.AggregateStateOrchestrator;
import com.dewdropper.command.CommandMapper;
import com.dewdropper.command.DefaultAggregateCommandMapper;
import com.dewdropper.streamstore.eventstore.EventStore;
import com.dewdropper.streamstore.repository.StreamStoreRepository;
import com.dewdropper.streamstore.serialize.JsonSerializer;
import com.dewdropper.streamstore.stream.PrefixStreamNameGenerator;
import com.dewdropper.structure.StreamNameGenerator;
import com.dewdropper.structure.serialize.EventSerializer;
import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.EventStoreDBClientSettings;
import com.eventstore.dbclient.EventStoreDBConnectionString;
import com.eventstore.dbclient.ParseError;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;

@Data
public class DewDropperSettings {
    private DewDropperSettings() {
    }

    private DewDropperProperties properties;
    private ObjectMapper objectMapper;
    private EventStoreDBClient eventStoreDBClient;
    private EventSerializer eventSerializer;
    private EventStore eventStore;
    private StreamNameGenerator streamNameGenerator;
    private StreamStoreRepository streamStoreRepository;
    private AggregateStateOrchestrator aggregateStateOrchestrator;
    private CommandMapper commandMapper;

    @Builder(buildMethodName = "create")
    public DewDropperSettings(DewDropperProperties properties, ObjectMapper objectMapper, EventStoreDBClient eventStoreDBClient, EventSerializer eventSerializer, CommandMapper commandMapper) {
        this.properties = properties;
        this.objectMapper = Optional.ofNullable(objectMapper)
            .orElse(defaultObjectMapper());
        try {
            if (properties == null) {
                throw new IllegalArgumentException("properties cannot be null");
            }
            this.eventStoreDBClient = Optional.ofNullable(eventStoreDBClient)
                .orElse(eventStoreDBClient(properties));
            this.eventStore = new EventStore(getEventStoreDBClient());
        } catch (ParseError e) {
            throw new IllegalArgumentException("Unable to parse EventStore connection", e);
        }
        this.eventSerializer = Optional.ofNullable(eventSerializer).orElse(new JsonSerializer(getObjectMapper()));
        this.streamNameGenerator = new PrefixStreamNameGenerator(getProperties().getStreamPrefix());
        this.streamStoreRepository = new StreamStoreRepository(getEventStore(), getStreamNameGenerator(), getEventSerializer());
        this.commandMapper = Optional.ofNullable(commandMapper).orElse(new DefaultAggregateCommandMapper(getStreamStoreRepository()));
        this.aggregateStateOrchestrator = new AggregateStateOrchestrator(getCommandMapper());
    }

    private EventStoreDBClient eventStoreDBClient(DewDropperProperties properties) throws ParseError {
        EventStoreDBClientSettings settings = EventStoreDBConnectionString.parseOrThrow(properties.getConnectionString());
        return EventStoreDBClient.create(settings);
    }

    public ObjectMapper defaultObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(new JavaTimeModule());

        // Sets up JSON to handle Instant
        objectMapper.configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, false);

        // We don't want null states since we don't know if the intent was to set to null.
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }

    public DewDropper start() {
        return new DewDropper(this);
    }
}
