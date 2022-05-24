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
public class InMemoryCacheProcessor<R> {
    private Class<?> cachedStateObjectType;
    private CacheManager cacheManager;
    private Cache<UUID, R> cache;
    private Map<String, Map<UUID, UUID>> cacheIndex;
    private String primaryCacheKeyName;
    private List<String> alternateCacheKeyNames;
    private Map<UUID, LinkedList<Message>> unprocessedMessages;

    public InMemoryCacheProcessor(Class<?> cachedStateObjectType, CacheManager cacheManager) {
        this.cachedStateObjectType = cachedStateObjectType;
        this.cacheManager = cacheManager;
        this.cache = cacheManager.createCache(this);
        this.primaryCacheKeyName = CacheUtils.getPrimaryCacheKey(cachedStateObjectType).getName();
        this.alternateCacheKeyNames = CacheUtils.getAlternateCacheKeys(cachedStateObjectType).stream().map(Field::getName).collect(toList());
        this.cacheIndex = new ConcurrentHashMap<>();
        this.unprocessedMessages = new ConcurrentHashMap<>();
        alternateCacheKeyNames.forEach(keyName -> cacheIndex.computeIfAbsent(keyName, key -> new ConcurrentHashMap<>()));
    }

    public <T extends Message> void process(T message) {
        log.info("Received message: {} to cache ", message);

        Optional<UUID> optId = CacheUtils.getCacheRootKey(message);
        optId.ifPresent(uuid -> {
            UUID id = uuid;
            if (DewdropReflectionUtils.hasField(message, primaryCacheKeyName)) {
                primaryCache(message, id);
            } else {
                alternateCache(message, id);
            }
        });
    }

    <T extends Message> void alternateCache(T message, UUID id) {
        alternateCacheKeyNames.forEach(alternateCacheKeyName -> processAlternateCache(message, id, alternateCacheKeyName));
    }

    <T extends Message> void processAlternateCache(T message, UUID alternateKey, String alternateCacheKeyName) {
        log.info("Received message: {} in alternate cache", message);
        Optional<UUID> optAlternateCacheKey = DewdropReflectionUtils.getFieldValue(message, alternateCacheKeyName);

        if (optAlternateCacheKey.isPresent()) {
            UUID alternateCacheKey = optAlternateCacheKey.get();
            if (isInCacheIndex(alternateCacheKeyName, alternateCacheKey)) {
                processAlternateKeyMessage(message, alternateCacheKeyName, alternateCacheKey);
            } else {
                notFoundInCacheIndex(message, alternateKey);
            }
        }
    }

    protected boolean isInCacheIndex(String alternateCacheKeyName, UUID alternateCacheKey) {
        return cacheIndex.get(alternateCacheKeyName).containsKey(alternateCacheKey);
    }

    <T extends Message> void notFoundInCacheIndex(T message, UUID id) {
        log.info("Key not found in CacheIndex - adding to unpublished", message);
        List<Message> unprocessed = unprocessedMessages.computeIfAbsent(id, key -> new LinkedList<>());
        unprocessed.add(message);
    }

    <T extends Message> void processAlternateKeyMessage(T message, String alternateCacheKeyName, UUID alternateCacheKey) {
        Map<UUID, UUID> index = cacheIndex.get(alternateCacheKeyName);
        R dto = cache.get(index.get(alternateCacheKey));
        log.info("Processing alternateKey message:{}", message);
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
        log.info("Processing message: {} for primary cache", message);
        ReadModelUtils.processOnEvent(dto, message);
        cache.put(id, dto);
    }

    <T extends Message> void initializePrimaryCache(T message, UUID id) {
        Optional<R> instance = DewdropReflectionUtils.createInstance(cachedStateObjectType);
        if (instance.isPresent()) {
            updatePrimaryCache(instance.get(), message, id);
        } else {
            log.error("Skipping processing of message:{} due to inability to create cachedStateObjectType:{} - Is it missing a default empty constructor?", message.getClass().getSimpleName(), cachedStateObjectType.getName());
        }
    }

    <T extends Message> void processCacheIndex(T message, UUID primaryCacheKey) {
        alternateCacheKeyNames.forEach(keyName -> {
            Optional<UUID> alternateKeyValue = DewdropReflectionUtils.getFieldValue(message, keyName);
            alternateKeyValue.ifPresent(uuid -> {
                cacheIndex.get(keyName).put(uuid, primaryCacheKey);
                unprocessedMessages.computeIfPresent(uuid, (key, messages) -> {
                    log.info("Processing unprocessed message:{}, primaryKey:{}, alternateKey:{}", message.getClass().getSimpleName(), primaryCacheKey, uuid);
                    processAlternateKeyMessage(messages.poll(), keyName, uuid);
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
