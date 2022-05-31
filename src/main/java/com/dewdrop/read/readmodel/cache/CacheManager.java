package com.dewdrop.read.readmodel.cache;

import static java.util.Objects.requireNonNull;

import com.dewdrop.utils.DewdropReflectionUtils;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CacheManager {
    private Map<Object, Cache> caches;

    public CacheManager() {
        caches = new ConcurrentHashMap<>();
    }

    public <T, R, U> Cache<T, R, U> createCache(Object key, Class<? extends Cache> cacheType) {
        requireNonNull(key, "Key object required");

        if (caches.containsKey(key)) {return getCache(key);}

        Optional<Cache<T, R, U>> instance = DewdropReflectionUtils.createInstance(cacheType);
        if (instance.isPresent()) {
            Cache<T, R, U> cache = instance.get();
            caches.put(key, cache);
            return cache;

        }
        throw new IllegalArgumentException("Unable to create instance of cacheType:" + cacheType);
    }

    public <T, R, U> Cache<T, R, U> getCache(Object key) {
        Cache cache = caches.get(key);
        if (cache == null) {
            log.error("Unable to find cache by key:{}", key);
            throw new IllegalArgumentException("Unable to find cache by key:" + key);
        }
        return cache;
    }

}
