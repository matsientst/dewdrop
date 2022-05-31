package com.dewdrop.read.readmodel.cache;

import static java.util.stream.Collectors.toList;

import com.dewdrop.structure.api.Message;
import com.dewdrop.utils.CacheUtils;
import com.dewdrop.utils.DewdropReflectionUtils;
import com.dewdrop.utils.ReadModelUtils;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class MapBackedInMemoryCacheProcessor<R> implements InMemoryCacheProcessor {
    private Class<?> cachedStateObjectType;
    private CacheManager cacheManager;
    private Cache<UUID, R, Map<UUID, R>> cache;
    private Map<String, Map<UUID, UUID>> cacheIndex;
    private String primaryCacheKeyName;
    private List<String> foreignCacheKeyNames;
    private Map<UUID, LinkedList<Message>> unprocessedMessages;

    public MapBackedInMemoryCacheProcessor(Class<?> cachedStateObjectType, CacheManager cacheManager) {
        this.cachedStateObjectType = cachedStateObjectType;
        this.cacheManager = cacheManager;
        this.cache = cacheManager.createCache(this, ConcurrentHashMapCache.class);
        this.primaryCacheKeyName = CacheUtils.getPrimaryCacheKey(cachedStateObjectType)
            .getName();
        this.foreignCacheKeyNames = CacheUtils.getForeignCacheKeys(cachedStateObjectType)
            .stream()
            .map(Field::getName)
            .collect(toList());
        this.cacheIndex = new ConcurrentHashMap<>();
        this.unprocessedMessages = new ConcurrentHashMap<>();
        foreignCacheKeyNames.forEach(keyName -> cacheIndex.computeIfAbsent(keyName, key -> new ConcurrentHashMap<>()));
    }

    public <T extends Message> void process(T message) {
        log.debug("Received message: {} to cache ", message);

        Optional<UUID> optId = CacheUtils.getCacheRootKey(message);
        optId.ifPresent(uuid -> {
            UUID id = uuid;
            if (DewdropReflectionUtils.hasField(message, primaryCacheKeyName)) {
                primaryCache(message, id);
            } else {
                foreignCache(message, id);
            }
        });
    }

    <T extends Message> void foreignCache(T message, UUID id) {
        foreignCacheKeyNames.forEach(foreignCacheKeyName -> processForeignCache(message, id, foreignCacheKeyName));
    }

    <T extends Message> void processForeignCache(T message, UUID foreignKey, String foreignCacheKeyName) {
        log.debug("Received message: {} in foreign cache", message);
        Optional<UUID> optForeignCacheKey = DewdropReflectionUtils.readFieldValue(message, foreignCacheKeyName);

        if (optForeignCacheKey.isPresent()) {
            UUID foreignCacheKey = optForeignCacheKey.get();
            if (isInCacheIndex(foreignCacheKeyName, foreignCacheKey)) {
                processForeignKeyMessage(message, foreignCacheKeyName, foreignCacheKey);
            } else {
                notFoundInCacheIndex(message, foreignKey);
            }
        }
    }

    protected boolean isInCacheIndex(String foreignCacheKeyName, UUID foreignCacheKey) {
        return cacheIndex.get(foreignCacheKeyName)
            .containsKey(foreignCacheKey);
    }

    <T extends Message> void notFoundInCacheIndex(T message, UUID id) {
        log.debug("Key not found in CacheIndex - adding to unpublished", message);
        List<Message> unprocessed = unprocessedMessages.computeIfAbsent(id, key -> new LinkedList<>());
        unprocessed.add(message);
    }

    <T extends Message> void processForeignKeyMessage(T message, String foreignCacheKeyName, UUID foreignCacheKey) {
        Map<UUID, UUID> index = cacheIndex.get(foreignCacheKeyName);
        R dto = cache.get(index.get(foreignCacheKey));
        log.debug("Processing foreignKey message:{}", message);
        ReadModelUtils.processOnEvent(dto, message);
    }

    <T extends Message> void primaryCache(T message, UUID id) {
        if (cache.containsKey(id)) {
            updatePrimaryCache(cache.get(id), message, id);
        } else if (CacheUtils.isCacheRoot(message)) {
            initializePrimaryCache(message, id);
        }
        processCacheIndex(message, id);
    }

    <T extends Message> void updatePrimaryCache(R dto, T message, UUID id) {
        log.debug("Processing message: {} for primary cache", message);
        ReadModelUtils.processOnEvent(dto, message);
        cache.put(id, dto);
    }

    <T extends Message> void initializePrimaryCache(T message, UUID id) {
        Optional<R> instance = DewdropReflectionUtils.createInstance(cachedStateObjectType);
        if (instance.isPresent()) {
            updatePrimaryCache(instance.get(), message, id);
        } else {
            log.error("Skipping processing of message:{} due to inability to create cachedStateObjectType:{} - Is it missing a default empty constructor?", message.getClass()
                .getSimpleName(), cachedStateObjectType.getName());
        }
    }

    <T extends Message> void processCacheIndex(T message, UUID primaryCacheKey) {
        foreignCacheKeyNames.forEach(keyName -> {
            Optional<UUID> foreignKeyValue = DewdropReflectionUtils.readFieldValue(message, keyName);
            foreignKeyValue.ifPresent(uuid -> {
                cacheIndex.get(keyName)
                    .put(uuid, primaryCacheKey);
                unprocessedMessages.computeIfPresent(uuid, (key, messages) -> {
                    log.debug("Processing unprocessed message:{}, primaryKey:{}, foreignKey:{}", message.getClass()
                        .getSimpleName(), primaryCacheKey, uuid);
                    processForeignKeyMessage(messages.poll(), keyName, uuid);
                    return null;
                });
            });
        });
    }

    public void put(UUID id, R item) {
        cache.put(id, item);
    }

    public Map<UUID, R> getAll() {
        return cache.getAll();
    }
}
