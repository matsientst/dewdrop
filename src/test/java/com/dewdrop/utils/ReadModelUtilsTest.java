package com.dewdrop.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.dewdrop.fixture.events.DewdropUserCreated;
import com.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetails;
import com.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetailsReadModel;
import com.dewdrop.fixture.readmodel.users.DewdropUser;
import com.dewdrop.fixture.readmodel.users.DewdropUsersReadModel;
import com.dewdrop.read.readmodel.annotation.DewdropCache;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@Log4j2
@ExtendWith(MockitoExtension.class)
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
        List<Method> methods = ReadModelUtils.getQueryHandlerMethods(readModel.getClass());
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

    @Test
    @DisplayName("getReadModelCacheField() - Given a targetClass, return field annotated with @DewdropCache")
    void getReadModelCacheField() {
        Field field = ReadModelUtils.getReadModelCacheField(DewdropUsersReadModel.class);
        assertThat(field, is(notNullValue()));
    }

    @Test
    @DisplayName("getReadModelCacheField() - Given a targetClass that does NOT annotate a field with @DewdropCache return null")
    void getReadModelCacheField_notFound() {
        Field field = ReadModelUtils.getReadModelCacheField(DewdropUser.class);
        assertThat(field, is(nullValue()));
    }

    @Test
    @DisplayName("getReadModelCacheField() - Given a targetClass that has more than 1 annotated fields with @DewdropCache return null")
    void getReadModelCacheField_tooMany() {
        Field field = ReadModelUtils.getReadModelCacheField(TooManyDewdropCaches.class);
        assertThat(field, is(nullValue()));
    }

    @Test
    @DisplayName("updateReadModelCacheField() - Given a readModel with an field annotated with @DewdropCache and a cacheProcessor update the field with the contents of cacheProcessor.getCache()")
    void updateReadModelCacheField() {
        DewdropUsersReadModel readModel = new DewdropUsersReadModel();
        ReadModelUtils.updateReadModelCacheField(readModel, new HashMap<>());
        assertThat(readModel.getCache(), is(notNullValue()));
    }

    @Test
    @DisplayName("updateReadModelCacheField() - Given a readModel with an field annotated with @DewdropCache and a cacheProcessor when writing and we get an IllegalAccessException return null")
    void updateReadModelCacheField_IllegalAccessException() {
        DewdropUsersReadModel readModel = new DewdropUsersReadModel();

        try (MockedStatic<FieldUtils> utilities = mockStatic(FieldUtils.class)) {
            utilities.when(() -> FieldUtils.getFieldsListWithAnnotation(any(Class.class), any(Class.class))).thenReturn(List.of(mock(Field.class)));
            utilities.when(() -> FieldUtils.writeField(any(Field.class), any(DewdropUsersReadModel.class), isA(List.class), isA(Boolean.class))).thenThrow(IllegalAccessException.class);

            ReadModelUtils.updateReadModelCacheField(readModel, new ArrayList<>());
            assertThat(readModel.getCache(), is(nullValue()));
        }
    }

    private class TooManyDewdropCaches {
        @DewdropCache
        String test;
        @DewdropCache
        String test2;
    }
}
