package com.dewdrop.utils;

import com.dewdrop.config.DependencyInjectionAdapter;
import java.util.Optional;

public class DependencyInjectionUtils {
    private DependencyInjectionUtils() {}

    public static DependencyInjectionAdapter dependencyInjectionAdapter;

    public static void setDependencyInjection(DependencyInjectionAdapter dependencyInjection) {
        if (dependencyInjection != null) {
            DependencyInjectionUtils.dependencyInjectionAdapter = dependencyInjection;
        }
    }

    public static <T> Optional<T> getInstance(Class<?> clazz) {
        if (dependencyInjectionAdapter != null) {
            T instance = dependencyInjectionAdapter.getBean(clazz);
            if (instance != null) { return Optional.of(instance); }
        }
        return DewdropReflectionUtils.createInstance(clazz);
    }
}
