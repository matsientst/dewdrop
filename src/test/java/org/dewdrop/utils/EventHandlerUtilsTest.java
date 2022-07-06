package org.dewdrop.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import org.dewdrop.fixture.customized.DewdropCommandService;
import org.dewdrop.fixture.events.DewdropAccountCreated;
import org.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import org.dewdrop.fixture.events.DewdropUserCreated;
import org.dewdrop.fixture.readmodel.AccountCreatedService;
import org.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetails;
import org.dewdrop.fixture.readmodel.users.DewdropUser;
import org.dewdrop.read.readmodel.ReadModel;
import org.dewdrop.read.readmodel.ReadModelWrapper;
import org.dewdrop.read.readmodel.annotation.EventHandler;
import org.dewdrop.read.readmodel.cache.MapBackedInMemoryCacheProcessor;
import org.dewdrop.structure.api.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class EventHandlerUtilsTest {
    @BeforeEach
    void setup() {
        ReflectionsConfigUtils.init("org.dewdrop");
    }

    @Test
    @DisplayName("getEventHandlers() - Given a ReadModel without an InMemoryCacheProcessor, when getEventHandlers() is called, then the ReadModel's supported events are returned")
    void getEventHandlers_noInMemoryCache() {
        ReadModel readModel = mock(ReadModel.class);
        doReturn(Optional.empty()).when(readModel).getInMemoryCacheProcessor();

        ReadModelWrapper readModelWrapper = mock(ReadModelWrapper.class);
        List<Class<DewdropAccountCreated>> eventHandlers = List.of(DewdropAccountCreated.class);
        doReturn(eventHandlers).when(readModelWrapper).getSupportedEvents();
        doReturn(readModelWrapper).when(readModel).getReadModelWrapper();

        List<Class<? extends Event>> results = EventHandlerUtils.getEventHandlers(readModel);
        assertThat(results, is(eventHandlers));
    }

    @Test
    @DisplayName("getEventHandlers() - Given a ReadModel with an InMemoryCacheProcessor, when getEventHandlers() is called, then look to the cachedStateObjectType for the supported events")
    void getEventHandlers_withInMemoryCache() {
        ReadModel readModel = mock(ReadModel.class);
        MapBackedInMemoryCacheProcessor inMemoryCacheProcessor = mock(MapBackedInMemoryCacheProcessor.class);
        doReturn(Optional.of(inMemoryCacheProcessor)).when(readModel).getInMemoryCacheProcessor();

        doReturn(DewdropAccountDetails.class).when(inMemoryCacheProcessor).getCachedStateObjectType();

        List<Class<? extends Event>> results = EventHandlerUtils.getEventHandlers(readModel);
        assertThat(results, containsInAnyOrder(DewdropAccountCreated.class, DewdropFundsAddedToAccount.class, DewdropUserCreated.class));
    }

    @Test
    @DisplayName("getEventToHandlerMethod() - Given a DTO class with @EventHandler annotations, when getEventToHandlerMethod() is called, then return the map of event to handler method")
    void getEventToHandlerMethod() {
        Map<Class<? extends Event>, Method> eventToHandlerMethod = EventHandlerUtils.getEventToHandlerMethod(DewdropAccountDetails.class, EventHandler.class);
        assertThat(eventToHandlerMethod.size(), is(3));
        assertThat(eventToHandlerMethod.get(DewdropAccountCreated.class), is(notNullValue()));
        assertThat(eventToHandlerMethod.get(DewdropFundsAddedToAccount.class), is(notNullValue()));
        assertThat(eventToHandlerMethod.get(DewdropUserCreated.class), is(notNullValue()));
    }

    @Test
    @DisplayName("getEventToHandlerMethod() - Given a DTO class with @EventHandler annotations but no parameter for the method, when getEventToHandlerMethod() is called, this method should be filtered out as it is invalid")
    void getEventToHandlerMethod_noParameter() {
        Map<Class<? extends Event>, Method> eventToHandlerMethod = EventHandlerUtils.getEventToHandlerMethod(NoParameterEventHandler.class, EventHandler.class);
        assertThat(eventToHandlerMethod.isEmpty(), is(true));
    }

    @Test
    @DisplayName("getEventToHandlerMethod() - Given a DTO class with @EventHandler annotations but a parameter that is not an event, when getEventToHandlerMethod() is called, this method should be filtered out as it is invalid")
    void getEventToHandlerMethod_notAnEvent() {
        Map<Class<? extends Event>, Method> eventToHandlerMethod = EventHandlerUtils.getEventToHandlerMethod(NotAnEventParameterEventHandler.class, EventHandler.class);
        assertThat(eventToHandlerMethod.isEmpty(), is(true));
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

    @Test
    @DisplayName("getOnEventMethod() - Given an object with a method annotated with @OnEvent and an event, when getOnEventMethod() is called, the method annotated with @OnEvent and the first parameter is the event is returned")
    void getOnEventMethod() {
        Optional<Method> onEventMethod = EventHandlerUtils.getOnEventMethod(AccountCreatedService.class, new DewdropAccountCreated());
        assertThat(onEventMethod.isPresent(), is(true));
    }


    private class NoParameterEventHandler {
        @EventHandler
        public void on() {}
    }

    private class NotAnEventParameterEventHandler {
        @EventHandler
        public void on(String test) {}
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
