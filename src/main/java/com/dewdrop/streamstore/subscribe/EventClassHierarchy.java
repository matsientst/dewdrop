package com.dewdrop.streamstore.subscribe;

import static java.util.Objects.requireNonNull;
import static org.reflections.scanners.Scanners.SubTypes;

import com.dewdrop.utils.ReflectionsConfigUtils;
import java.util.Set;

public class EventClassHierarchy {
    private EventClassHierarchy() {}

    public static Set<Class<?>> getMyChildren(Class<?> type) {
        requireNonNull(type, "Type is required");

        return ReflectionsConfigUtils.REFLECTIONS.get(SubTypes.of(type).asClass());
    }
}
