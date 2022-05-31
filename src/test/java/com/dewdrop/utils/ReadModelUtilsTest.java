package com.dewdrop.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import com.dewdrop.fixture.events.DewdropUserCreated;
import com.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetails;
import com.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetailsReadModel;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@Log4j2
class ReadModelUtilsTest {
    @BeforeEach
    void setup() {
        ReflectionsConfigUtils.init("com.dewdrop");
    }

    @Test
    @DisplayName("getAnnotatedReadModels() - Get all classes annotated with the @ReadModel annotation")
    void getAnnotatedReadModels() {
        List<Class<?>> annotatedReadModels = ReadModelUtils.getAnnotatedReadModels();
        assertThat(annotatedReadModels.isEmpty(), is(false));
        ReadModelUtils.getAnnotatedReadModels();
    }

    @Test
    @DisplayName("getAnnotatedReadModels() - If we have no methods annotated with @ReadModel we return an empty set")
    void getAnnotatedReadModels_verifyCacheIsUsed() {
        ReadModelUtils.clear();
        try (MockedStatic<DewdropAnnotationUtils> utilities = mockStatic(DewdropAnnotationUtils.class)) {
            utilities.when(() -> DewdropAnnotationUtils.getAnnotatedClasses(any())).thenReturn(new HashSet<>());

            assertThat(ReadModelUtils.getAnnotatedReadModels().isEmpty(), is(true));
        }
    }

    @Test
    @DisplayName("getQueryHandlerMethods() - get the methods that are decorated with @QueryHandler")
    void getQueryHandlerMethods() {
        DewdropAccountDetailsReadModel readModel = new DewdropAccountDetailsReadModel();
        List<Method> methods = ReadModelUtils.getQueryHandlerMethods(readModel);
        assertThat(methods.isEmpty(), is(false));
    }

    @Test
    @DisplayName("processOnEvent() - Given a target and an event, we should be able to call the on(Event) method on the target")
    void processOnEvent() {
        DewdropAccountDetails cacheTarget = new DewdropAccountDetails();
        DewdropUserCreated userCreated = new DewdropUserCreated(UUID.randomUUID(), "Test");
        ReadModelUtils.processOnEvent(cacheTarget, userCreated);
        assertThat(cacheTarget.getUsername(), is(userCreated.getUsername()));
    }
}
