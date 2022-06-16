package com.dewdrop.read.readmodel;

import com.dewdrop.read.readmodel.cache.InMemoryCacheProcessor;
import com.dewdrop.read.readmodel.stream.Stream;
import com.dewdrop.structure.api.Event;
import com.dewdrop.utils.EventHandlerUtils;
import com.dewdrop.utils.ReadModelUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Data
@Log4j2
public class ReadModel<T extends Event> {
    private Object readModel;
    private Optional<InMemoryCacheProcessor> inMemoryCacheProcessor;
    protected List<Stream<T>> streams = new ArrayList<>();

    public ReadModel(Object readModel, Optional<InMemoryCacheProcessor> inMemoryCacheProcessor) {
        this.readModel = readModel;
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
        log.debug("handling message {}", message);

        if (inMemoryCacheProcessor.isPresent()) {
            inMemoryCacheProcessor.get().process(message);
        }

        EventHandlerUtils.callEventHandler(readModel, message);
    }

    public Consumer<T> handler() {
        return this::process;
    }

    public void handle(T message) {
        process(message);
    }

    public <R> R getCachedItems() {
        if (inMemoryCacheProcessor.isPresent()) { return inMemoryCacheProcessor.get().getCache(); }
        return null;
    }

    public Object getReadModel() {
        return readModel;
    }

    public void addStream(Stream<T> stream) {
        this.streams.add(stream);
    }


    public void updateState() {
        streams.forEach(stream -> stream.updateState());
        if (inMemoryCacheProcessor.isPresent()) {
            ReadModelUtils.updateReadModelCacheField(readModel, inMemoryCacheProcessor.get().getCache());
        }
    }
}
