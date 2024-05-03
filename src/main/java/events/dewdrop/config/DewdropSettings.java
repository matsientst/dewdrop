package events.dewdrop.config;

import events.dewdrop.Dewdrop;
import events.dewdrop.aggregate.AggregateStateOrchestrator;
import events.dewdrop.command.CommandHandlerMapper;
import events.dewdrop.command.CommandMapper;
import events.dewdrop.config.ascii.Ascii;
import events.dewdrop.read.readmodel.DefaultAnnotationReadModelMapper;
import events.dewdrop.read.readmodel.ReadModelMapper;
import events.dewdrop.read.readmodel.stream.StreamFactory;
import events.dewdrop.streamstore.process.AggregateStateCommandProcessor;
import events.dewdrop.structure.StreamNameGenerator;
import events.dewdrop.utils.ReflectionsConfigUtils;
import events.dewdrop.read.readmodel.QueryStateOrchestrator;
import events.dewdrop.read.readmodel.ReadModelFactory;
import events.dewdrop.streamstore.eventstore.EventStore;
import events.dewdrop.streamstore.process.AggregateRootLifecycle;
import events.dewdrop.streamstore.serialize.JsonSerializer;
import events.dewdrop.streamstore.stream.PrefixStreamNameGenerator;
import events.dewdrop.structure.datastore.StreamStore;
import events.dewdrop.structure.serialize.EventSerializer;
import events.dewdrop.utils.DependencyInjectionUtils;
import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.EventStoreDBClientSettings;
import com.eventstore.dbclient.EventStoreDBConnectionString;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Data
@Log4j2
public class DewdropSettings {
    private DewdropSettings() {}

    private DewdropProperties properties;
    private ObjectMapper objectMapper;
    private EventStoreDBClient eventStoreDBClient;
    private EventSerializer eventSerializer;
    private StreamStore streamStore;
    private StreamNameGenerator streamNameGenerator;
    private AggregateStateCommandProcessor aggregateStateCommandProcessor;
    private AggregateStateOrchestrator aggregateStateOrchestrator;
    private QueryStateOrchestrator queryStateOrchestrator;
    private CommandMapper commandMapper;
    private ReadModelMapper readModelMapper;
    private StreamFactory streamFactory;
    private ReadModelFactory readModelFactory;
    private AggregateRootLifecycle streamProcessor;

    @Builder(buildMethodName = "create")
    public DewdropSettings(DewdropProperties properties, ObjectMapper objectMapper, EventStoreDBClient eventStoreDBClient, EventSerializer eventSerializer, CommandMapper commandMapper, ReadModelMapper readModelMapper,
                    DependencyInjectionAdapter dependencyInjectionAdapter) {
        Ascii.writeAscii();
        this.properties = properties;
        this.objectMapper = Optional.ofNullable(objectMapper).orElse(defaultObjectMapper());
        try {
            if (properties == null) { throw new IllegalArgumentException("properties cannot be null"); }
            this.eventStoreDBClient = Optional.ofNullable(eventStoreDBClient).orElse(eventStoreDBClient(properties));
            this.streamStore = new EventStore(getEventStoreDBClient());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse EventStore connection", e);
        }
        DependencyInjectionUtils.setDependencyInjection(dependencyInjectionAdapter);
        ReflectionsConfigUtils.init(getProperties().getPackageToScan(), getProperties().getPackageToExclude());
        this.eventSerializer = Optional.ofNullable(eventSerializer).orElse(new JsonSerializer(getObjectMapper()));
        this.streamNameGenerator = new PrefixStreamNameGenerator(getProperties().getStreamPrefix());

        // Streams
        this.streamFactory = new StreamFactory(getStreamStore(), getEventSerializer(), getStreamNameGenerator());
        this.streamProcessor = new AggregateRootLifecycle(getStreamFactory());

        // Read Models (before commands)
        this.readModelMapper = Optional.ofNullable(readModelMapper).orElse(new DefaultAnnotationReadModelMapper());
        this.readModelFactory = new ReadModelFactory(getStreamStore(), getEventSerializer(), getStreamFactory());
        getReadModelMapper().init(getReadModelFactory());
        this.queryStateOrchestrator = new QueryStateOrchestrator(getReadModelMapper());

        this.aggregateStateCommandProcessor = new AggregateStateCommandProcessor(getStreamProcessor());
        this.commandMapper = Optional.ofNullable(commandMapper).orElse(new CommandHandlerMapper());
        this.aggregateStateOrchestrator = new AggregateStateOrchestrator(getCommandMapper(), getAggregateStateCommandProcessor());
    }

    private EventStoreDBClient eventStoreDBClient(DewdropProperties properties) {
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
        Dewdrop dewdrop = new Dewdrop(this);
        log.info("Dewdrop successfully started");
        return dewdrop;
    }
}
