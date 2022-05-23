package com.dewdrop.read.readmodel.cache;

import static java.util.Objects.requireNonNull;

import com.dewdrop.utils.DewdropReflectionUtils;
import java.util.Map;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.map.HashedMap;

@Log4j2
public class CacheManager {
    private Map<Object, Cache> caches;
    private Class<?> cacheType;

    public CacheManager(Class<?> cacheType) {
        caches = new HashedMap<>();
        this.cacheType = cacheType;
    }

    public <T, R> Cache<T, R> createCache(Object key) {
        requireNonNull(key, "Key object required");

        if (caches.containsKey(key)) {
            return getCache(key);
        }

        Optional<Cache<T, R>> instance = DewdropReflectionUtils.createInstance(cacheType);
        if (instance.isPresent()) {
            Cache<T, R> cache = instance.get();
            caches.put(key, cache);
            return cache;

        }
        throw new IllegalArgumentException("Unable to create instance of cacheType:" + cacheType);
    }

    public <T, R> Cache<T, R> getCache(Object key) {
        Cache cache = caches.get(key);
        if (cache == null) {
            log.error("Unable to find cache by key:{}", key);
            throw new IllegalArgumentException("Unable to find cache by key:" + key);
        }
        return cache;
    }

}
