package events.dewdrop.read.readmodel.cache;


import events.dewdrop.utils.DewdropReflectionUtils;
import events.dewdrop.utils.EventHandlerUtils;
import events.dewdrop.structure.api.Message;
import java.util.Optional;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class SingleItemInMemoryCache<T> implements InMemoryCacheProcessor {
    private Class<?> cachedStateObjectType;
    private T cache;

    public SingleItemInMemoryCache(Class<?> cachedStateObjectType) {
        this.cachedStateObjectType = cachedStateObjectType;
        Optional<T> optInstance = DewdropReflectionUtils.createInstance(cachedStateObjectType);
        if (optInstance.isPresent()) {
            cache = optInstance.get();
        }
    }

    public <T extends Message> void process(T message) {
        log.debug("Received message: {} to cache ", message);

        EventHandlerUtils.callEventHandler(this.cache, message);
    }

    @Override
    public T getCache() {
        return cache;
    }
}
