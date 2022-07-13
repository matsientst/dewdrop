package events.dewdrop.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import events.dewdrop.read.readmodel.query.QueryHandler;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import org.apache.commons.lang3.reflect.MethodUtils;
import events.dewdrop.api.result.Result;
import events.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetailsReadModel;
import events.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummaryQuery;
import events.dewdrop.fixture.readmodel.users.DewdropUser;
import events.dewdrop.fixture.readmodel.users.DewdropUsersReadModel;
import events.dewdrop.fixture.readmodel.users.GetUserByIdQuery;
import events.dewdrop.read.readmodel.ReadModelWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryHandlerUtilsTest {
    ReadModelWrapper readModelWrapper;
    GetUserByIdQuery query;
    DewdropUser user;
    Method method;
    Field field;
    Object readModel;

    @BeforeEach
    void setup() {
        readModel = new DewdropUsersReadModel();
        query = new GetUserByIdQuery(UUID.randomUUID());
        user = new DewdropUser();
        method = MethodUtils.getMatchingMethod(DewdropUsersReadModel.class, "query", GetUserByIdQuery.class);
        readModelWrapper = ReadModelWrapper.of(DewdropUsersReadModel.class).get();
        field = readModelWrapper.getCacheField().get();
        readModel = readModelWrapper.getReadModel();
    }

    @Test
    @DisplayName("getMethodForQuery() - Given an object with a method annotated with @QueryHandler and a query object, then return Optional.of(Method)")
    void getMethodForQuery() {
        Method method = mock(Method.class);
        when(method.getParameterTypes()).thenReturn(new Class[] {DewdropAccountSummaryQuery.class});
        try (MockedStatic<DewdropAnnotationUtils> utilities = mockStatic(DewdropAnnotationUtils.class)) {
            utilities.when(() -> DewdropAnnotationUtils.getAnnotatedMethods(any(), any(Class.class))).thenReturn(Set.of(method));

            Optional<Method> methodForQuery = QueryHandlerUtils.getMethodForQuery(DewdropAccountDetailsReadModel.class, new DewdropAccountSummaryQuery());
            assertThat(methodForQuery.isPresent(), is(true));
        }
    }

    @Test
    @DisplayName("getMethodForQuery() - Given an object with a method annotated with @QueryHandler with no parameters and a query object, then return Optional.empty()")
    void getMethodForQuery_noParameters() {
        Method method = mock(Method.class);
        when(method.getParameterTypes()).thenReturn(new Class[] {});
        try (MockedStatic<DewdropAnnotationUtils> utilities = mockStatic(DewdropAnnotationUtils.class)) {
            utilities.when(() -> DewdropAnnotationUtils.getAnnotatedMethods(any(), any(Class.class))).thenReturn(Set.of(method));

            Optional<Method> methodForQuery = QueryHandlerUtils.getMethodForQuery(NoMethodQueryHandler.class, new DewdropAccountSummaryQuery());
            assertThat(methodForQuery.isEmpty(), is(true));
        }
    }


    @Test
    @DisplayName("callQueryHandler() - Given an object with a method annotated with @QueryHandler and an query, the object will call the method annotated with @QueryHandler")
    void callQueryHandler() {
        user.setUserId(query.getUserId());

        ReadModelUtils.updateReadModelCacheField(field, readModelWrapper.getReadModel(), Map.of(user.getUserId(), user));
        try (MockedStatic<DewdropAnnotationUtils> utilities = mockStatic(DewdropAnnotationUtils.class)) {
            utilities.when(() -> DewdropAnnotationUtils.getAnnotatedMethods(any(), any(Class.class))).thenReturn(Set.of(method));
            Result<DewdropUser> result = QueryHandlerUtils.callQueryHandler(readModelWrapper, query);
            assertThat(result.get().getUserId(), is(query.getUserId()));
        }
    }

    @Test
    @DisplayName("callQueryHandler() - Given an object with a method annotated with @QueryHandler and a query, the object returned will already be a result")
    void callQueryHandler_returnResult() throws InvocationTargetException, IllegalAccessException {
        user.setUserId(query.getUserId());
        ReadModelUtils.updateReadModelCacheField(field, readModel, Map.of(user.getUserId(), user));
        Method spy = spy(method);
        doReturn(new Class[] {GetUserByIdQuery.class}).when(spy).getParameterTypes();

        try (MockedStatic<DewdropAnnotationUtils> utilities = mockStatic(DewdropAnnotationUtils.class)) {
            try (MockedStatic<DewdropReflectionUtils> reflectionUtils = mockStatic(DewdropReflectionUtils.class)) {
                utilities.when(() -> DewdropAnnotationUtils.getAnnotatedMethods(any(), any(Class.class))).thenReturn(Set.of(spy));
                reflectionUtils.when(() -> DewdropReflectionUtils.getMatchingMethod(any(Method.class), any())).thenReturn(Optional.of(method));

                Result<DewdropUser> result = QueryHandlerUtils.callQueryHandler(readModelWrapper, query);
                assertThat(result.isValuePresent(), is(true));
            }
        }
    }

    @Test
    @DisplayName("callQueryHandler() - Given an object with a method annotated with @QueryHandler and a query and a null response, the object returned will be an empty result")
    void callQueryHandler_returnNull() throws InvocationTargetException, IllegalAccessException {
        user.setUserId(query.getUserId());
        ReadModelUtils.updateReadModelCacheField(field, readModel, Map.of(user.getUserId(), user));
        Method spy = spy(method);
        doReturn(new Class[] {GetUserByIdQuery.class}).when(spy).getParameterTypes();
        doReturn(null).when(spy).invoke(any(), any());
        try (MockedStatic<DewdropAnnotationUtils> utilities = mockStatic(DewdropAnnotationUtils.class)) {
            try (MockedStatic<DewdropReflectionUtils> reflectionUtils = mockStatic(DewdropReflectionUtils.class)) {
                utilities.when(() -> DewdropAnnotationUtils.getAnnotatedMethods(any(), any(Class.class))).thenReturn(Set.of(spy));
                reflectionUtils.when(() -> DewdropReflectionUtils.getMatchingMethod(any(Method.class), any())).thenReturn(Optional.of(spy));

                Result<DewdropUser> result = QueryHandlerUtils.callQueryHandler(readModelWrapper, query);
                assertThat(result.isEmpty(), is(true));
            }
        }
    }

    @Test
    @DisplayName("callQueryHandler() - Given an object with a method annotated with @QueryHandler and an query, the object will call the method annotated with @QueryHandler")
    void callQueryHandler_IllegalArgumentException() throws InvocationTargetException, IllegalAccessException {
        Method method = mock(Method.class);
        doReturn(new Class[] {GetUserByIdQuery.class}).when(method).getParameterTypes();
        doThrow(IllegalArgumentException.class).when(method).invoke(any(), any());
        DewdropUser instance = new DewdropUser();

        try (MockedStatic<DewdropAnnotationUtils> utilities = mockStatic(DewdropAnnotationUtils.class)) {
            try (MockedStatic<DewdropReflectionUtils> reflectionUtils = mockStatic(DewdropReflectionUtils.class)) {
                utilities.when(() -> DewdropAnnotationUtils.getAnnotatedMethods(any(), any(Class.class))).thenReturn(Set.of(method));
                reflectionUtils.when(() -> DewdropReflectionUtils.getMatchingMethod(any(Method.class), any())).thenReturn(Optional.of(method));
                QueryHandlerUtils.callQueryHandler(readModelWrapper, query);
                assertThat(instance.getUserId(), is(nullValue()));
            }
        }
    }

    @Test
    @DisplayName("callQueryHandler() - Given an object without a method annotated with @QueryHandler and an query, the object will not be updated")
    void callQueryHandler_noQueryHandlerMethod() {
        readModelWrapper.setOriginalReadModelClass(NoMethodQueryHandler.class);
        readModelWrapper.setReadModel(new NoMethodQueryHandler());
        Result<Boolean> result = QueryHandlerUtils.callQueryHandler(readModelWrapper, query);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    @DisplayName("callQueryHandler() - Given a ReadModelWrapper and a query, when the ReadModelWrapper originalReadModelCLass does not have a matching method in the ReadModel, then return Result.empty()")
    void callQueryHandler_noMatchingMethod() {
        readModelWrapper.setReadModel(new NoParameterQueryHandler());
        Result<Boolean> result = QueryHandlerUtils.callQueryHandler(readModelWrapper, query);
        assertThat(result.isEmpty(), is(true));
    }


    private class NoParameterQueryHandler {
        @QueryHandler
        public void on() {}
    }

    @Data
    private class NoMethodQueryHandler {
        UUID userId;

        public Boolean on(GetUserByIdQuery query) {
            return true;
        }
    }
}
