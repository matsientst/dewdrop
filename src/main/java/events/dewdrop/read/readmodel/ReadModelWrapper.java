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

@Data
public class ReadModelWrapper {
    private Class<?> originalReadModelClass;
    private Object readModel;
    private Map<Class<? extends Event>, Method> eventToEventHandlerMethod = new ConcurrentHashMap<>();
    private Optional<Field> cacheField;

    private ReadModelWrapper(Class<?> originalReadModelClass, Object readModel) {
        this.originalReadModelClass = originalReadModelClass;
        this.readModel = readModel;
        assignEventHandlers(this.eventToEventHandlerMethod, () -> EventHandlerUtils.getEventToEventHandlerMethod(this.originalReadModelClass), originalReadModelClass, readModel);
        this.cacheField = ReadModelUtils.getMatchingReadModelCacheField(this);

    }

    private void assignEventHandlers(final Map<Class<? extends Event>, Method> eventToHandler, Supplier<Map<Class<? extends Event>, Method>> getEventToHandlers, Class<?> originalReadModelClass, Object readModel) {
        Map<Class<? extends Event>, Method> mapOfHandlers = getEventToHandlers.get();
        if (readModel.getClass().isAssignableFrom(originalReadModelClass)) {
            eventToHandler.putAll(mapOfHandlers);
        } else {
            mapOfHandlers.forEach((eventClass, method) -> {
                Optional<Method> proxiedMethod = DewdropReflectionUtils.getMatchingMethod(method, readModel);
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
        if (eventToEventHandlerMethod.containsKey(message.getClass())) {
            Method method = eventToEventHandlerMethod.get(message.getClass());
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
        return eventToEventHandlerMethod.keySet().stream().collect(toList());
    }
}
