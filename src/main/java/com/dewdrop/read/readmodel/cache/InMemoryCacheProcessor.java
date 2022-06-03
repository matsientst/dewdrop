package com.dewdrop.read.readmodel.cache;

import com.dewdrop.structure.api.Message;

public interface InMemoryCacheProcessor {
    <T extends Message> void process(T message);

    <T> T getCache();
}
