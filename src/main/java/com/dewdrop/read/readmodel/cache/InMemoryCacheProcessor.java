package com.dewdrop.read.readmodel.cache;

import static java.util.stream.Collectors.toList;

import com.dewdrop.structure.api.Message;
import com.dewdrop.utils.CacheUtils;
import com.dewdrop.utils.DewdropReflectionUtils;
import com.dewdrop.utils.ReadModelUtils;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.reflect.FieldUtils;
@Log4j2
@Data
public class InMemoryCacheProcessor<R> {
    private Class<?> cachedStateObjectType;
    private CacheManager cacheManager;
    private Cache<UUID, R> cache;
    private Map<String, Map<UUID, UUID>> cacheIndex;
    private String primaryCacheKeyName;
    private List<String> alternateCacheKeyNames;

    public InMemoryCacheProcessor(Class<?> cachedStateObjectType, CacheManager cacheManager) {
        this.cachedStateObjectType = cachedStateObjectType;
        this.cacheManager = cacheManager;
        this.cache = cacheManager.createCache(this);
        this.primaryCacheKeyName = CacheUtils.getPrimaryCacheKey(cachedStateObjectType).getName();
        this.alternateCacheKeyNames = CacheUtils.getAlternateCacheKeys(cachedStateObjectType).stream().map(Field::getName).collect(toList());
        this.cacheIndex = new HashedMap<>();
    }

    public <T extends Message> void process(T message) {
        log.info("processing message {}", message);

        Optional<UUID> optId = CacheUtils.getCacheRootKey(message);
        if (optId.isPresent()) {
            UUID id = optId.get();
            if(DewdropReflectionUtils.hasField(message, primaryCacheKeyName)) {
                primaryCache(message, id);
            } else {
                alternateCache(message, id);
            }


        }
    }

    private <T extends Message> void alternateCache(T message, UUID id) {
        alternateCacheKeyNames.stream().forEach(alternateCacheKeyName ->{
            processAlternateCache(message, id, alternateCacheKeyName);
        });
    }

    private <T extends Message> void processAlternateCache(T message, UUID id, String alternateCacheKeyName) {
        Field field = FieldUtils.getField(message.getClass(), alternateCacheKeyName, true);
        if(field != null) {
            Optional<UUID> optAlternateCacheKey = (Optional<UUID>) DewdropReflectionUtils.getFieldValue(message, alternateCacheKeyName);
            if(optAlternateCacheKey.isPresent() && cacheIndex.containsKey(alternateCacheKeyName)) {
                foundInCacheIndex(message, alternateCacheKeyName, optAlternateCacheKey);
            } else {
                notFoundInCacheIndex(message, id, alternateCacheKeyName);
            }
        }
    }

    private <T extends Message> void notFoundInCacheIndex(T message, UUID id, String alternateCacheKeyName) {
        Map<UUID, UUID> index = new HashedMap<>();
        cache.getAll().values().stream().forEach(dto -> {
            Optional<UUID> dtoAlternateId = (Optional<UUID>) DewdropReflectionUtils.getFieldValue(dto, alternateCacheKeyName);
            if(dtoAlternateId.isPresent()) {
                UUID alternateId = dtoAlternateId.get();
                log.info("alternateId:{}, id:{}", alternateId, id);
                if(alternateId.equals(id)) {
                    index.put(alternateId, id);
                    ReadModelUtils.processOnEvent(dto, message);
                }
            }
        });
    }

    private <T extends Message> void foundInCacheIndex(T message, String alternateCacheKeyName, Optional<UUID> optAlternateCacheKey) {
        Map<UUID, UUID> index = cacheIndex.get(alternateCacheKeyName);
        UUID key = optAlternateCacheKey.get();
        if(index.containsKey(key)) {
            R dto =  cache.get(index.get(key));
            ReadModelUtils.processOnEvent(dto, message);
        } else {
            cache.getAll().values().stream().forEach(dto -> {
                Optional<UUID> dtoAlternateId = (Optional<UUID>) DewdropReflectionUtils.getFieldValue(dto, alternateCacheKeyName);
                if(dtoAlternateId.isPresent()) {
                    UUID primaryId = (UUID) DewdropReflectionUtils.getFieldValue(dto, primaryCacheKeyName);
                    index.put(dtoAlternateId.get(), primaryId);
                    ReadModelUtils.processOnEvent(dto, message);
                }
            });
        }
    }

    private <T extends Message> void primaryCache(T message, UUID id) {
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

        } else if (cache.containsKey(id)) {
            R dto = cache.get(id);
            ReadModelUtils.processOnEvent(dto, message);
        }
    }

    public void addToCache(UUID id, R item) {
        if (id != null) {
            cache.put(id, item);
        }
    }

    public void put(UUID id, R item) {
        cache.put(id, item);
    }

    public Map<UUID, R> getAll() {
        return cache.getAll();
    }
}
