package com.dewdrop.read.readmodel;

import com.dewdrop.read.readmodel.cache.InMemoryCacheProcessor;
import com.dewdrop.read.readmodel.stream.Stream;
import com.dewdrop.structure.api.Message;
import com.dewdrop.utils.EventHandlerUtils;
import com.dewdrop.utils.ReadModelUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Data
@Log4j2
public class ReadModel<T extends Message> {
    private Class<?> cachedStateObjectType;
    private Object readModel;
    private InMemoryCacheProcessor inMemoryCacheProcessor;
    protected List<Stream<T>> streams = new ArrayList<>();

    public ReadModel(Object readModel, Class<?> cachedStateObjectType, InMemoryCacheProcessor inMemoryCacheProcessor) {
        this.cachedStateObjectType = cachedStateObjectType;
        this.readModel = readModel;
        this.inMemoryCacheProcessor = inMemoryCacheProcessor;
    }
    // on demand read through cache
    // live subscription to category stream and throw away any events for account not currently in cache.
    // Only create the read model when we are querying for that specific accountId


    protected void subscribe() {
        getStreams().forEach(Stream::subscribe);
    }

    protected void process(T message) {
        log.debug("handling message {}", message);

        inMemoryCacheProcessor.process(message);

        EventHandlerUtils.callEventHandler(readModel, message, inMemoryCacheProcessor.getCache()
            .getAll());
    }

    public Consumer<T> handler() {
        return this::process;
    }

    public void handle(T message) {
        process(message);
    }

    public <R> R getCachedItems() {
        return inMemoryCacheProcessor.getAll();
    }

    public Object getReadModel() {
        return readModel;
    }

    public void addStream(Stream<T> stream) {
        this.streams.add(stream);
    }


    public void updateState() {
        streams.forEach(stream -> stream.updateState());
        ReadModelUtils.updateReadModelCacheField(readModel, inMemoryCacheProcessor);
    }
}
