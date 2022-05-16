package com.dewdropper.streamstore.subscribe;

import static java.util.Objects.requireNonNull;
import static org.reflections.scanners.Scanners.SubTypes;

import java.util.Set;
import org.reflections.Reflections;

public class EventClassHierarchy {
    private EventClassHierarchy() {}

    private static Reflections reflections = new Reflections("com.dewdropper");

    public static Set<Class<?>> getMeAndMyChildren(Class<?> type) {
        requireNonNull(type, "Type is required");

        Set<Class<?>> subTypes = reflections.get(SubTypes.of(type).asClass());
        subTypes.add(type);
        return subTypes;
    }
}
