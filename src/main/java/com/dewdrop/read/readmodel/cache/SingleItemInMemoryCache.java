package com.dewdrop.read.readmodel.cache;


import com.dewdrop.structure.api.Message;
import com.dewdrop.utils.DewdropReflectionUtils;
import com.dewdrop.utils.EventHandlerUtils;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class SingleItemInMemoryCache<T> implements InMemoryCacheProcessor {
    private Class<?> cachedStateObjectType;
    private CacheManager cacheManager;
    private Cache<T, T, T> cache;
    private T instance;

    public SingleItemInMemoryCache(Class<?> cachedStateObjectType, CacheManager cacheManager) {
        this.cachedStateObjectType = cachedStateObjectType;
        this.cache = cacheManager.createCache(this, SingleItemCache.class);
        Optional<T> optInstance = DewdropReflectionUtils.createInstance(cachedStateObjectType);
        if (optInstance.isPresent()) {
            instance = optInstance.get();
            this.cache.put(instance, instance);
        }
    }

    public <T extends Message> void process(T message) {
        log.debug("Received message: {} to cache ", message);

        EventHandlerUtils.callEventHandler(this.cache.get(instance), message);
    }

    @Override
    public Cache getCache() {
        return cache;
    }

    @Override
    public T getAll() {
        return cache.getAll();
    }
}
