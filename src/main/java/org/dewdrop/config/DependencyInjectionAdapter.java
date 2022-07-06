package org.dewdrop.config;

public interface DependencyInjectionAdapter {
    public <T> T getBean(Class<?> clazz);
}
