package com.dewdrop.read.readmodel;

import com.dewdrop.read.readmodel.cache.CacheManager;
import com.dewdrop.read.readmodel.cache.InMemoryCacheProcessor;
import com.dewdrop.read.readmodel.stream.Stream;
import com.dewdrop.structure.api.Message;
import com.dewdrop.utils.ReadModelUtils;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.reflect.MethodUtils;

@Log4j2
@Data
public class CacheableReadModel<R> extends AbstractReadModel {
    private Class<?> cachedStateObjectType;
    private Object readModel;
    private InMemoryCacheProcessor<R> inMemoryCacheProcessor;

    public CacheableReadModel(Object readModel, Class<?> cachedStateObjectType, CacheManager cacheManager) {
        super();
        this.cachedStateObjectType = cachedStateObjectType;
        this.readModel = readModel;
        this.inMemoryCacheProcessor = new InMemoryCacheProcessor<>(cachedStateObjectType, cacheManager);

    }

    protected void subscribe() {
        getStreams().forEach(Stream::subscribe);
    }

    protected <T extends Message> void process(T message) {
        log.info("handling message {}", message);

        inMemoryCacheProcessor.process(message);

        if (MethodUtils.getMatchingMethod(readModel.getClass(), "on", message.getClass()) != null) {
            ReadModelUtils.processOnEvent(readModel, message);
        }
    }

    public <T extends Message> Consumer<T> handler() {
        return message -> process(message);
    }

    @Override
    public <T extends Message> void handle(T message) {
        process(message);
    }

    protected void addToCache(UUID id, R item) {
        if (id != null) {
            inMemoryCacheProcessor.put(id, item);
        }
    }

    public Map<UUID, R> getCachedItems() {
        return (Map<UUID, R>) inMemoryCacheProcessor.getAll();
    }

    public Object getReadModel() {
        return readModel;
    }
}
