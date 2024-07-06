package events.dewdrop.read.readmodel;

import static java.util.stream.Collectors.toList;

import events.dewdrop.read.readmodel.annotation.Stream;
import events.dewdrop.utils.DewdropReflectionUtils;
import events.dewdrop.utils.EventHandlerUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.Data;
import events.dewdrop.structure.api.Event;
import events.dewdrop.utils.DependencyInjectionUtils;
import events.dewdrop.utils.ReadModelUtils;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class ReadModelWrapper {
    private Class<?> originalReadModelClass;
    private Object readModel;
    private Map<Class<? extends Event>, Method> eventToReadModelEventHandler = new ConcurrentHashMap<>();
    private Map<Class<? extends Event>, Method> eventToOnEventEventHandler = new ConcurrentHashMap<>();
    private Optional<Field> cacheField;

    private ReadModelWrapper(Class<?> originalReadModelClass, Object readModel) {
        log.info("Constructing ReadModelWrapper for originalReadModelClass:{}, readModel:{}", originalReadModelClass.getSimpleName(), readModel.getClass().getSimpleName());
        this.originalReadModelClass = originalReadModelClass;
        this.readModel = readModel;
        this.cacheField = ReadModelUtils.getMatchingReadModelCacheField(this);
        assignEventHandlers(this.eventToReadModelEventHandler, () -> EventHandlerUtils.getEventToEventHandlerMethod(this.originalReadModelClass), originalReadModelClass, readModel);
        assignEventHandlers(this.eventToOnEventEventHandler, () -> EventHandlerUtils.getEventToOnEventHandlerMethod(this.originalReadModelClass), originalReadModelClass, readModel);
    }

    private void assignEventHandlers(final Map<Class<? extends Event>, Method> eventToHandler, Supplier<Map<Class<? extends Event>, Method>> getEventToHandlers, Class<?> originalReadModelClass, Object readModel) {
        Map<Class<? extends Event>, Method> mapOfHandlers = getEventToHandlers.get();
        if (readModel.getClass().isAssignableFrom(originalReadModelClass)) {
            eventToHandler.putAll(mapOfHandlers);
        } else {
            mapOfHandlers.forEach((eventClass, method) -> {
                Optional<Method> proxiedMethod = DewdropReflectionUtils.getMatchingMethod(method, readModel.getClass());
                if (proxiedMethod.isPresent()) {
                    eventToHandler.put(eventClass, proxiedMethod.get());
                }
            });
        }
    }

    public static <T> Optional<ReadModelWrapper> of(Class<?> originalReadModelClass) {
        Optional<ReadModel<Event>> instance = DependencyInjectionUtils.getInstance(originalReadModelClass);
        if (instance.isPresent()) { return Optional.of(new ReadModelWrapper(originalReadModelClass, instance.get())); }
        return Optional.empty();
    }


    public List<Stream> getStreamAnnotations() {
        Stream[] streams = originalReadModelClass.getAnnotationsByType(Stream.class);
        return Arrays.asList(streams);
    }

    public <T extends Event> void callEventHandlers(T message) {
        if (eventToReadModelEventHandler.containsKey(message.getClass())) {
            Method method = eventToReadModelEventHandler.get(message.getClass());
            DewdropReflectionUtils.callMethod(readModel, method, message);
        }
        if (eventToOnEventEventHandler.containsKey(message.getClass())) {
            Method method = eventToOnEventEventHandler.get(message.getClass());
            DewdropReflectionUtils.callMethod(readModel, method, message);
        }
    }

    public <T> void updateReadModelCache(T cache) {
        if (cacheField.isPresent()) {
            ReadModelUtils.updateReadModelCacheField(cacheField.get(), readModel, cache);
        }
    }

    // Returning a list of all the events that the read model supports.
    public List<Class<? extends Event>> getSupportedEvents() {
        return eventToReadModelEventHandler.keySet().stream().collect(toList());
    }

    public String toString() {
        return originalReadModelClass.getSimpleName();
    }
}
