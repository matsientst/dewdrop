package events.dewdrop.read.readmodel.cache;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import events.dewdrop.structure.api.Event;
import events.dewdrop.structure.api.Message;
import events.dewdrop.utils.CacheUtils;
import events.dewdrop.utils.DewdropReflectionUtils;
import events.dewdrop.utils.ReadModelUtils;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class ImprovedMapBackedInMemoryCacheProcessor<R> implements InMemoryCacheProcessor {
    private Class<?> cachedStateObjectType;
    private Class<? extends Event> primaryEvent;
    private Map<UUID, R> cache;
    private final List<Field> foreignCacheKeyFields;
    private Map<UUID, List<Message>> primaryStashedMessages;
    private Map<UUID, List<Message>> foreignStashedMessages;
    private Map<UUID, UUID> foreignCacheIndex;

    public ImprovedMapBackedInMemoryCacheProcessor(Class<?> cachedStateObjectType) {
        this.cachedStateObjectType = cachedStateObjectType;
        this.cache = new ConcurrentHashMap<>();
        this.primaryEvent = CacheUtils.getCreationEventClass(cachedStateObjectType);
        this.foreignCacheKeyFields = CacheUtils.getForeignCacheKeyFields(cachedStateObjectType);
        this.primaryStashedMessages = new ConcurrentHashMap();
        this.foreignStashedMessages = new ConcurrentHashMap();
        this.foreignCacheIndex = new ConcurrentHashMap<>();
    }

    @Override
    public <T extends Message> void process(T message) {
        log.debug("Received message: {} to cache ", message.getClass().getSimpleName());

        Optional<UUID> optId = CacheUtils.getCacheRootKey(message);
        optId.ifPresent(uuid -> {
            boolean isPrimary = isPrimary(message);
            boolean isCreationEvent = isCreationEvent(message);
            boolean cacheHasKey = cache.containsKey(uuid);
            R dto = cache.get(uuid);
            if (isPrimary && isCreationEvent) {
                dto = initializePrimaryCache(message, uuid);
            } else if (isPrimary && cacheHasKey) {
                dto = updatePrimaryCache(dto, message, uuid);
            } else if (isPrimary && !cacheHasKey) {
                primaryStashedMessages.computeIfAbsent(uuid, id -> new ArrayList<>()).add(message);
            } else if (!isPrimary) {
                foreignCache(message, uuid);
            }
            if (dto != null) {
                updateForeignCacheIndex(dto, uuid);
            }
            processStashedMessages(dto, message, uuid);
        });
    }

    private <T extends Message> void processStashedMessages(R dto, T message, UUID uuid) {
        processForeignStashedMessages();
        processPrimaryStashedMessages(uuid);
    }

    private <T extends Message> void processForeignStashedMessages() {
        if (foreignStashedMessages.isEmpty()) { return; }
        Collection<List<Message>> values = foreignStashedMessages.values();
        values.forEach(stashedMessages -> {
            List<Message> toRemove = new ArrayList<>();
            stashedMessages.forEach(message -> {
                foreignCacheKeyFields.forEach(field -> {
                    processForeignCache(message, field, false).ifPresent(uuid -> toRemove.add(message));
                });

            });
            stashedMessages.removeAll(toRemove);
        });
    }

    private <T extends Message> void processPrimaryStashedMessages(UUID uuid) {
        if (primaryStashedMessages.isEmpty()) { return; }
        Collection<List<Message>> values = primaryStashedMessages.values();
        values.forEach(stashedMessages -> {
            List<Message> toRemove = new ArrayList<>();
            stashedMessages.forEach(message -> {
                Optional<UUID> cacheRootKey = CacheUtils.getCacheRootKey(message);

                if (cacheRootKey.isPresent() && uuid.equals(cacheRootKey.get())) {
                    R dto = cache.get(cacheRootKey.get());
                    if (dto != null) {
                        updatePrimaryCache(dto, message, cacheRootKey.get());
                        toRemove.add(message);
                    }
                }
            });
            stashedMessages.removeAll(toRemove);
        });
    }

    // If we see that we have found a foreignKey in our dto field make sure to add it to index
    private void updateForeignCacheIndex(R dto, UUID uuid) {
        foreignCacheKeyFields.stream().forEach(field -> {
            UUID dtoForeignKeyValue = DewdropReflectionUtils.readFieldValue(field, dto);
            if (dtoForeignKeyValue != null) {
                foreignCacheIndex.put(dtoForeignKeyValue, uuid);
            }
        });
    }

    private <T extends Message> boolean isCreationEvent(T message) {
        return CacheUtils.isCacheRoot(message);
    }

    private <T extends Message> boolean isPrimary(T message) {
        return this.primaryEvent.isAssignableFrom(message.getClass());
    }

    <T extends Message> R updatePrimaryCache(R dto, Message message, UUID id) {
        log.debug("Processing message: {} for primary cache", message);
        ReadModelUtils.processOnEvent(dto, message);
        cache.put(id, dto);
        return dto;
    }

    <T extends Message> R initializePrimaryCache(T message, UUID id) {
        Optional<R> instance = DewdropReflectionUtils.createInstance(cachedStateObjectType);
        if (instance.isPresent()) {
            R dto = instance.get();
            return updatePrimaryCache(dto, message, id);
        } else {
            log.error("Skipping processing of message:{} due to inability to create cachedStateObjectType:{} - Is it missing a default empty constructor?", message.getClass().getSimpleName(), cachedStateObjectType.getName());
        }
        return null;
    }

    <T extends Message> void foreignCache(T message, UUID id) {
        foreignCacheKeyFields.forEach(foreignCacheKeyName -> processForeignCache(message, foreignCacheKeyName, true));
    }

    <T extends Message> Optional<Boolean> processForeignCache(T message, Field foreignCacheKeyField, boolean cache) {
        log.debug("Received message: {} in foreign cache", message);
        Optional<UUID> optForeignCacheKey = CacheUtils.getForeignCacheEventKeyValue(message, foreignCacheKeyField);

        if (optForeignCacheKey.isPresent()) {
            UUID uuidFromMessage = optForeignCacheKey.get();
            if (isForeignKeyValueInIndex(uuidFromMessage)) {
                processForeignKeyMessage(message, uuidFromMessage);
                return Optional.of(true);
            } else if (cache) {
                notFoundInCacheIndex(uuidFromMessage, message);
            }
        }
        return Optional.empty();
    }

    <T extends Message> void notFoundInCacheIndex(UUID uuidFromMessage, Message message) {
        log.debug("Key not found in CacheIndex - adding to unpublished", message);
        List<Message> unprocessed = foreignStashedMessages.computeIfAbsent(uuidFromMessage, key -> new ArrayList<>());
        unprocessed.add(message);
    }

    protected boolean isForeignKeyValueInIndex(UUID foreignCacheKeyValue) {
        return foreignCacheIndex.containsKey(foreignCacheKeyValue);
    }

    <T extends Message> boolean processForeignKeyMessage(T message, UUID foreignCacheKey) {
        UUID key = foreignCacheIndex.get(foreignCacheKey);
        if (key != null) {
            R dto = cache.get(key);
            log.debug("Processing foreignKey message:{}", message);
            if (dto != null) {
                ReadModelUtils.processOnEvent(dto, message);
                return true;
            }
        }
        return false;
    }


    @Override
    public Map<UUID, R> getCache() {
        return cache;
    }

    @Override
    public Class<?> getCachedStateObjectType() {
        return cachedStateObjectType;
    }
}
