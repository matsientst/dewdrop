package com.dewdrop.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.dewdrop.fixture.events.DewdropUserCreated;
import com.dewdrop.fixture.readmodel.users.DewdropUser;
import com.dewdrop.read.readmodel.annotation.EventHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class EventHandlerUtilsTest {
    @BeforeEach
    void setup() {
        ReflectionsConfigUtils.init("com.dewdrop");
    }

    @Test
    @DisplayName("getFirstParameterForEventHandlerMethods() - Given an object, get the first parameter for each method annotated with @EventHandler")
    void getFirstParameterForEventHandlerMethods() {
        List<Class<?>> firstParameters = EventHandlerUtils.getFirstParameterForEventHandlerMethods(DewdropUser.class);
        assertThat(firstParameters.get(0), is(DewdropUserCreated.class));
    }

    @Test
    @DisplayName("getFirstParameterForEventHandlerMethods() - Given an object with a method annotated with @EventHandler but no parameters, return empty list")
    void getFirstParameterForEventHandlerMethods_noParameters() {
        List<Class<?>> firstParameters = EventHandlerUtils.getFirstParameterForEventHandlerMethods(NoParameterEventHandler.class);
        assertThat(firstParameters.isEmpty(), is(true));
    }

    @Test
    @DisplayName("getFirstParameterForEventHandlerMethods() - Given an object without a method annotated with @EventHandler, return empty list")
    void getFirstParameterForEventHandlerMethods_noMethods() {
        List<Class<?>> firstParameters = EventHandlerUtils.getFirstParameterForEventHandlerMethods(NoMethodEventHandler.class);
        assertThat(firstParameters.isEmpty(), is(true));
    }

    @Test
    @DisplayName("callEventHandler() - Given an object with a method annotated with @EventHandler and an event, the object will call the method annotated with @EventHandler")
    void callEventHandler() {
        DewdropUser instance = new DewdropUser();
        DewdropUserCreated event = new DewdropUserCreated(UUID.randomUUID(), "test");
        EventHandlerUtils.callEventHandler(instance, event);
        assertThat(instance.getUserId(), is(event.getUserId()));
        assertThat(instance.getUsername(), is(event.getUsername()));
    }

    @Test
    @DisplayName("callEventHandler() - Given an object with a method annotated with @EventHandler and an event, the object will call the method annotated with @EventHandler")
    void callEventHandler_IllegalArgumentException() throws InvocationTargetException, IllegalAccessException {
        Method method = mock(Method.class);
        doReturn(new Class[] {DewdropUserCreated.class}).when(method).getParameterTypes();
        doThrow(IllegalArgumentException.class).when(method).invoke(any(), any());
        DewdropUser instance = new DewdropUser();
        DewdropUserCreated event = new DewdropUserCreated(UUID.randomUUID(), "test");

        try (MockedStatic<DewdropAnnotationUtils> utilities = mockStatic(DewdropAnnotationUtils.class)) {
            utilities.when(() -> DewdropAnnotationUtils.getAnnotatedMethods(any(), any(Class.class))).thenReturn(Set.of(method));

            EventHandlerUtils.callEventHandler(instance, event);
            assertThat(instance.getUserId(), is(nullValue()));
        }
    }

    @Test
    @DisplayName("callEventHandler() - Given an object without a method annotated with @EventHandler and an event, the object will not be updated")
    void callEventHandler_noEventHandlerMethod() {
        NoMethodEventHandler instance = new NoMethodEventHandler();
        DewdropUserCreated event = new DewdropUserCreated(UUID.randomUUID(), "test");
        EventHandlerUtils.callEventHandler(instance, event);
        assertThat(instance.getUserId(), is(nullValue()));
    }


    @Test
    @DisplayName("callEventHandler() - Given an object with a method annotated with @EventHandler and an event and a cache object, the object will call the method annotated with @EventHandler")
    void callEventHandler_secondArg() {
        TwoParameterEventHandler instance = new TwoParameterEventHandler();
        DewdropUserCreated event = new DewdropUserCreated(UUID.randomUUID(), "test");
        EventHandlerUtils.callEventHandler(instance, event, new ArrayList<>());
        assertThat(instance.getUserId(), is(event.getUserId()));
        assertThat(instance.getUsers(), is(notNullValue()));
    }


    private class NoParameterEventHandler {
        @EventHandler
        public void on() {}
    }

    @Data
    private class NoMethodEventHandler {
        UUID userId;

        public void on(DewdropUserCreated event) {
            this.userId = event.getUserId();
        }
    }

    @Data
    private class TwoParameterEventHandler {
        UUID userId;
        List<DewdropUser> users;

        @EventHandler
        public void on(DewdropUserCreated event, List<DewdropUser> users) {
            this.userId = event.getUserId();
            this.users = users;

        }
    }
}
