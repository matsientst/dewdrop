package org.dewdrop.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.dewdrop.config.DependencyInjectionAdapter;
import org.dewdrop.fixture.events.DewdropUserCreated;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;

class DependencyInjectionUtilsTest {
    DependencyInjectionAdapter dependencyInjection;

    @BeforeEach
    void setup() {
        dependencyInjection = spy(new TestDependencyInjectionAdapter());
        DependencyInjectionUtils.setDependencyInjection(dependencyInjection);
    }

    @Test
    @DisplayName("setDependencyInjection() - Given a dependency injection adapter, When the method is called, Then the adapter is set")
    void setDependencyInjection() {
        assertThat(DependencyInjectionUtils.dependencyInjectionAdapter, is(dependencyInjection));
    }


    @Test
    @DisplayName("getInstance() - Given a dependency injection adapter, when getInstance() is called, Then getBean() is called on the adapter")
    void getInstance() {
        doReturn(new Object()).when(dependencyInjection).getBean(any(Class.class));
        assertThat(DependencyInjectionUtils.getInstance(DewdropUserCreated.class).isPresent(), is(true));
        verify(dependencyInjection, times(1)).getBean(ArgumentMatchers.any(Class.class));
    }

    @Test
    @DisplayName("getInstance() - Given a null object from the dependency injection adapter, when getInstance() is called, Then DewdropReflectionUtils.createInstance() is called")
    void getInstance_nullFromDependencyInjector() {
        doReturn(null).when(dependencyInjection).getBean(any(Class.class));

        try (MockedStatic<DewdropReflectionUtils> utilities = mockStatic(DewdropReflectionUtils.class)) {
            utilities.when(() -> DewdropReflectionUtils.createInstance(any(Class.class))).thenReturn(Optional.of(new Object()));

            assertThat(DependencyInjectionUtils.getInstance(DewdropUserCreated.class).isPresent(), is(true));
            verify(dependencyInjection, times(1)).getBean(ArgumentMatchers.any(Class.class));
        }
    }

    @Test
    @DisplayName("getInstance() - Given a null dependency injection adapter, when getInstance() is called, Then DewdropReflectionUtils.createInstance() is called")
    void getInstance_nullDependencyInjector() {
        DependencyInjectionUtils.dependencyInjectionAdapter = null;

        try (MockedStatic<DewdropReflectionUtils> utilities = mockStatic(DewdropReflectionUtils.class)) {
            utilities.when(() -> DewdropReflectionUtils.createInstance(any(Class.class))).thenReturn(Optional.of(new Object()));

            assertThat(DependencyInjectionUtils.getInstance(DewdropUserCreated.class).isPresent(), is(true));
            verify(dependencyInjection, times(0)).getBean(ArgumentMatchers.any(Class.class));
        }
    }

    private class TestDependencyInjectionAdapter implements DependencyInjectionAdapter {
        @Override
        public <T> T getBean(Class<?> clazz) {
            return (T) new String();
        }
    }
}
