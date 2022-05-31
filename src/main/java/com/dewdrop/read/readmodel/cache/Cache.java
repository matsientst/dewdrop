package com.dewdrop.read.readmodel.cache;

public interface Cache<T, R, U> {

    boolean containsKey(T key);

    void put(T key, R value);

    R get(T key);

    U getAll();
}
