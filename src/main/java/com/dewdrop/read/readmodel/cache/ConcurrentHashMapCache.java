package com.dewdrop.read.readmodel.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentHashMapCache<T, R> implements Cache<T, R, Map<T, R>> {
    private final ConcurrentMap<T, R> cacheMap = new ConcurrentHashMap();

    @Override
    public boolean containsKey(T key) {
        return cacheMap.containsKey(key);
    }

    @Override
    public void put(T key, R value) {
        cacheMap.put(key, value);
    }

    @Override
    public R get(T key) {
        return cacheMap.get(key);
    }

    @Override
    public Map<T, R> getAll() {
        return cacheMap;
    }
}
