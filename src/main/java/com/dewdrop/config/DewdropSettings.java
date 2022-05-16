package com.dewdrop.config;

import com.dewdrop.Dewdrop;
import com.dewdrop.aggregate.AggregateStateOrchestrator;
import com.dewdrop.command.CommandMapper;
import com.dewdrop.command.DefaultAggregateCommandMapper;
import com.dewdrop.streamstore.eventstore.EventStore;
import com.dewdrop.streamstore.repository.StreamStoreRepository;
import com.dewdrop.streamstore.serialize.JsonSerializer;
import com.dewdrop.streamstore.stream.PrefixStreamNameGenerator;
import com.dewdrop.structure.StreamNameGenerator;
import com.dewdrop.structure.serialize.EventSerializer;
import com.dewdrop.utils.AnnotationReflection;
import com.dewdrop.utils.ReflectionsConfigUtils;
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
public class DewdropSettings {
    private DewdropSettings() {
    }

    private DewdropProperties properties;
    private ObjectMapper objectMapper;
    private EventStoreDBClient eventStoreDBClient;
    private EventSerializer eventSerializer;
    private EventStore eventStore;
    private StreamNameGenerator streamNameGenerator;
    private StreamStoreRepository streamStoreRepository;
    private AggregateStateOrchestrator aggregateStateOrchestrator;
    private CommandMapper commandMapper;

    @Builder(buildMethodName = "create")
    public DewdropSettings(DewdropProperties properties, ObjectMapper objectMapper, EventStoreDBClient eventStoreDBClient, EventSerializer eventSerializer, CommandMapper commandMapper) {
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
        ReflectionsConfigUtils.init(getProperties().getPackageToScan());
        this.eventSerializer = Optional.ofNullable(eventSerializer).orElse(new JsonSerializer(getObjectMapper()));
        this.streamNameGenerator = new PrefixStreamNameGenerator(getProperties().getStreamPrefix());
        this.streamStoreRepository = new StreamStoreRepository(getEventStore(), getStreamNameGenerator(), getEventSerializer());
        this.commandMapper = Optional.ofNullable(commandMapper).orElse(new DefaultAggregateCommandMapper(getStreamStoreRepository()));
        this.aggregateStateOrchestrator = new AggregateStateOrchestrator(getCommandMapper());
    }

    private EventStoreDBClient eventStoreDBClient(DewdropProperties properties) throws ParseError {
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

    public Dewdrop start() {
        return new Dewdrop(this);
    }
}
