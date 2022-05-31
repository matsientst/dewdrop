package com.dewdrop.read.readmodel.cache;

import com.dewdrop.utils.DewdropReflectionUtils;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class SingleItemCache<T> implements Cache<T, T, T> {
    private T cachedItem;

    @Override
    public boolean containsKey(T key) {
        return cachedItem != null && key.getClass()
            .equals(cachedItem.getClass());
    }

    @Override
    public void put(T key, T value) {
        if (cachedItem == null) {
            cachedItem = (T) DewdropReflectionUtils.createInstance(value.getClass());
        }
        this.cachedItem = value;
    }

    @Override
    public T get(T key) {
        return cachedItem;
    }

    @Override
    public T getAll() {
        return cachedItem;
    }
}
