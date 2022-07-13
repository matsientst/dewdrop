package events.dewdrop.read.readmodel;

import events.dewdrop.utils.CacheUtils;
import events.dewdrop.read.readmodel.cache.InMemoryCacheProcessor;
import events.dewdrop.read.readmodel.stream.Stream;
import events.dewdrop.structure.api.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Data
@Log4j2
public class ReadModel<T extends Event> {
    private ReadModelWrapper readModelWrapper;
    private Optional<InMemoryCacheProcessor> inMemoryCacheProcessor;
    protected List<Stream<T>> streams = new ArrayList<>();

    public ReadModel(ReadModelWrapper readModelWrapper, Optional<InMemoryCacheProcessor> inMemoryCacheProcessor) {
        this.readModelWrapper = readModelWrapper;
        this.inMemoryCacheProcessor = inMemoryCacheProcessor;
    }
    // on demand read through cache
    // live subscription to category stream and throw away any events for account not currently in
    // cache.
    // Only create the read model when we are querying for that specific accountId


    protected void subscribe() {
        getStreams().forEach(Stream::subscribe);
    }

    protected void process(T message) {
        Optional<UUID> cacheRootKey = CacheUtils.getCacheRootKey(message);
        log.info("handling event type: {} - id:{}, version:{}", message.getClass().getSimpleName(), cacheRootKey.orElse(null), message.getVersion());

        inMemoryCacheProcessor.ifPresent(memoryCacheProcessor -> memoryCacheProcessor.process(message));
        readModelWrapper.callEventHandlers(message);
    }

    public Consumer<T> handler() {
        return this::process;
    }

    public void handle(T message) {
        process(message);
    }

    public <R> R getCachedItems() {
        return inMemoryCacheProcessor.<R>map(InMemoryCacheProcessor::getCache).orElse(null);
    }

    // public Object getReadModel() {
    // return readModel;
    // }

    public void addStream(Stream<T> stream) {
        this.streams.add(stream);
    }


    public void updateState() {
        streams.forEach(Stream::updateState);
        inMemoryCacheProcessor.ifPresent(memoryCacheProcessor -> readModelWrapper.updateReadModelCache(memoryCacheProcessor.getCache()));
    }

    public String getTargetClassSimpleName() {
        return readModelWrapper.getOriginalReadModelClass().getSimpleName();
    }
}
