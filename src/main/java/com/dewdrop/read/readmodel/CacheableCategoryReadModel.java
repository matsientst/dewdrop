package com.dewdrop.read.readmodel;

import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.serialize.EventSerializer;
import com.dewdrop.utils.CacheUtils;
import com.dewdrop.utils.DewdropReflectionUtils;
import com.dewdrop.utils.ReadModelUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.reflect.MethodUtils;

@Log4j2
@Data
public class CacheableCategoryReadModel<T extends Message, R> extends CategoryReadModel<T> {
    private Class<?> cachedStateObjectType;
    private Class<?> aggregateRootType;
    private Object readModel;

    public CacheableCategoryReadModel(Object readModel, Class<?> messageType, Class<?> cachedStateObjectType, Class<?> aggregateRootType, StreamStore streamStoreConnection, EventSerializer eventSerializer) {
        super(messageType, streamStoreConnection, eventSerializer);
        this.cachedStateObjectType = cachedStateObjectType;
        this.readModel = readModel;
        this.aggregateRootType = aggregateRootType;
    }

    private Map<UUID, R> cachedItems = new HashMap<>();

    protected void readAndSubscribe() {
        readAndSubscribe(aggregateRootType.getSimpleName(), handler(), messageType);
    }

    @Override
    protected void process(T message) {
        log.info("handling message {}", message);

        Optional<UUID> optId = CacheUtils.getCacheRootKey(message);
        if (optId.isPresent()) {
            UUID id = optId.get();
            if (CacheUtils.isCacheRoot(message)) {
                Optional<Object> instance = DewdropReflectionUtils.createInstance(cachedStateObjectType);
                if (instance.isPresent()) {
                    R dto = (R) instance.get();
                    ReadModelUtils.processOnEvent(dto, message);
                    addToCache(id, dto);
                } else {
                    log.error("skipping processing of message:{} due to inability to create cachedStateObjectType:{}", message.getClass()
                        .getSimpleName(), cachedStateObjectType);
                }

            } else if (cachedItems.containsKey(id)) {
                R dto = cachedItems.get(id);
                ReadModelUtils.processOnEvent(dto, message);
            }
        }

        if (MethodUtils.getMatchingMethod(readModel.getClass(), "on", message.getClass()) != null) {
            ReadModelUtils.processOnEvent(readModel, message);
        }

    }


    @Override
    public void handle(T message) {
        process(message);
    }

    protected void addToCache(UUID id, R item) {
        if (id != null) {
            cachedItems.put(id, item);
        }
    }

    public Map<UUID, R> getCachedItems() {
        return cachedItems;
    }

    public Object getReadModel() {
        return readModel;
    }
}
