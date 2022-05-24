package com.dewdrop.read.readmodel;

import com.dewdrop.read.readmodel.cache.CacheManager;
import com.dewdrop.read.readmodel.cache.InMemoryCacheProcessor;
import com.dewdrop.read.readmodel.stream.Stream;
import com.dewdrop.structure.api.Message;
import com.dewdrop.utils.ReadModelUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.reflect.MethodUtils;

@Data
@Log4j2
public class ReadModel<T> {
    private Class<?> cachedStateObjectType;
    private Object readModel;
    private InMemoryCacheProcessor<T> inMemoryCacheProcessor;
    protected List<Stream<? super Message>> streams = new ArrayList<>();

    protected ReadModel(Object readModel, Class<?> cachedStateObjectType, CacheManager cacheManager) {
        this.cachedStateObjectType = cachedStateObjectType;
        this.readModel = readModel;
        this.inMemoryCacheProcessor = new InMemoryCacheProcessor<>(cachedStateObjectType, cacheManager);
    }


    protected void subscribe() {
        getStreams().forEach(Stream::subscribe);
    }

    protected <T extends Message> void process(T message) {
        log.debug("handling message {}", message);

        inMemoryCacheProcessor.process(message);

        if (MethodUtils.getMatchingMethod(readModel.getClass(), "on", message.getClass()) != null) {
            ReadModelUtils.processOnEvent(readModel, message);
        }
    }

    public <R extends Message> Consumer<R> handler() {
        return message -> process(message);
    }

    public <R extends Message> void handle(R message) {
        process(message);
    }

    protected void addToCache(UUID id, T item) {
        if (id != null) {
            inMemoryCacheProcessor.put(id, item);
        }
    }

    public Map<UUID, T> getCachedItems() {
        return (Map<UUID, T>) inMemoryCacheProcessor.getAll();
    }

    public Object getReadModel() {
        return readModel;
    }

    public void addStream(Stream stream) {
        this.streams.add(stream);
    }


    public void updateState() {
        streams.forEach(stream -> stream.updateState());
    }
}
