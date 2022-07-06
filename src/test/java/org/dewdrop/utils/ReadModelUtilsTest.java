package org.dewdrop.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.dewdrop.fixture.events.DewdropUserCreated;
import org.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetails;
import org.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetailsReadModel;
import org.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummaryReadModel;
import org.dewdrop.fixture.readmodel.users.DewdropUser;
import org.dewdrop.fixture.readmodel.users.DewdropUsersReadModel;
import org.dewdrop.read.readmodel.ReadModelWrapper;
import org.dewdrop.read.readmodel.annotation.DewdropCache;
import org.dewdrop.read.readmodel.cache.MapBackedInMemoryCacheProcessor;
import org.dewdrop.read.readmodel.cache.SingleItemInMemoryCache;
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
        ReflectionsConfigUtils.init("org.dewdrop");
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
    @DisplayName("updateReadModelCacheField() - Given a readModel with a field annotated with @DewdropCache and a cacheProcessor update the field with the contents of cacheProcessor.getCache()")
    void updateReadModelCacheField() {
        ReadModelWrapper readModelWrapper = ReadModelWrapper.of(DewdropUsersReadModel.class).get();
        DewdropUsersReadModel readModel = (DewdropUsersReadModel) readModelWrapper.getReadModel();

        ReadModelUtils.updateReadModelCacheField(readModelWrapper.getCacheField().get(), readModelWrapper.getReadModel(), new HashMap<>());
        assertThat(readModel.getCache(), is(notNullValue()));
    }

    @Test
    @DisplayName("updateReadModelCacheField() - Given a readModel with an field annotated with @DewdropCache and a cacheProcessor when writing and we get an IllegalAccessException return null")
    void updateReadModelCacheField_IllegalAccessException() {
        DewdropUsersReadModel readModel = new DewdropUsersReadModel();
        Field field = mock(Field.class);
        try (MockedStatic<FieldUtils> utilities = mockStatic(FieldUtils.class)) {
            utilities.when(() -> FieldUtils.getFieldsListWithAnnotation(any(Class.class), any(Class.class))).thenReturn(List.of(mock(Field.class)));
            utilities.when(() -> FieldUtils.writeField(any(Field.class), any(DewdropUsersReadModel.class), isA(List.class), isA(Boolean.class))).thenThrow(IllegalAccessException.class);

            ReadModelUtils.updateReadModelCacheField(field, readModel, new ArrayList<>());
            assertThat(readModel.getCache(), is(nullValue()));
        }
    }

    @Test
    @DisplayName("createInMemoryCache() - Given a read model class with an @DewdropCache of a map, create an MapBackedInMemoryCacheProcessor cache")
    void createInMemoryCache() {
        assertThat(ReadModelUtils.createInMemoryCache(DewdropUsersReadModel.class).get().getClass(), is(MapBackedInMemoryCacheProcessor.class));
    }

    @Test
    @DisplayName("createInMemoryCache() - Given a read model class with an @DewdropCache of a single item, create an SingleItemInMemoryCache cache")
    void createInMemoryCache_singleItemInMemoryCache() {
        assertThat(ReadModelUtils.createInMemoryCache(DewdropAccountSummaryReadModel.class).get().getClass(), is(SingleItemInMemoryCache.class));
    }

    @Test
    @DisplayName("createInMemoryCache() - Given a read model class without a @DewdropCache, return an empty Optional")
    void createInMemoryCache_noCache() {
        assertThat(ReadModelUtils.createInMemoryCache(String.class).isEmpty(), is(true));
    }

    @Test
    @DisplayName("isEphemeral() - Given a read model class with an @ReadModel, when ephemeral is true, return true")
    void isEphemeral() {
        assertThat(ReadModelUtils.isEphemeral(DewdropAccountDetailsReadModel.class), is(true));
    }

    @Test
    @DisplayName("isEphemeral() - Given a read model class with an @ReadModel, when ephemeral is false, return false")
    void isEphemeral_false() {
        assertThat(ReadModelUtils.isEphemeral(DewdropAccountSummaryReadModel.class), is(false));
    }

    @Test
    @DisplayName("isEphemeral() - Given a read model class without a @ReadModel, return false")
    void isEphemeral_noAnnotation() {
        assertThat(ReadModelUtils.isEphemeral(String.class), is(false));
    }

    private class TooManyDewdropCaches {
        @DewdropCache
        String test;
        @DewdropCache
        String test2;
    }
}
