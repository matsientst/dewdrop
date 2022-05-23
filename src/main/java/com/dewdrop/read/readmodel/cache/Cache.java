package com.dewdrop.read.readmodel.cache;

import java.util.List;
import java.util.Map;

public interface Cache<T, R> {

    boolean containsKey(T key);

    void put(T key, R value);

    R get(T key);

    Map<T, R> getAll();
}
