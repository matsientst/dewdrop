package events.dewdrop.read.readmodel.cache;

import events.dewdrop.structure.api.Message;

public interface InMemoryCacheProcessor {
    <T extends Message> void process(T message);

    <T> T getCache();

    Class<?> getCachedStateObjectType();
}
