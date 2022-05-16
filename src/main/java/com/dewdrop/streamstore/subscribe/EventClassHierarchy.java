package com.dewdrop.streamstore.subscribe;

import static java.util.Objects.requireNonNull;
import static org.reflections.scanners.Scanners.SubTypes;

import com.dewdrop.utils.ReflectionsConfigUtils;
import java.util.Set;
import org.reflections.Reflections;

public class EventClassHierarchy {
    private EventClassHierarchy() {}

    public static Set<Class<?>> getMeAndMyChildren(Class<?> type) {
        requireNonNull(type, "Type is required");

        Set<Class<?>> subTypes = ReflectionsConfigUtils.REFLECTIONS.get(SubTypes.of(type).asClass());
        subTypes.add(type);
        return subTypes;
    }
}
