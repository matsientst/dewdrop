package com.dewdrop.utils;

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

import com.dewdrop.api.result.Result;
import com.dewdrop.api.result.ResultException;
import com.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetailsReadModel;
import com.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummaryQuery;
import com.dewdrop.fixture.readmodel.users.DewdropUser;
import com.dewdrop.fixture.readmodel.users.DewdropUsersReadModel;
import com.dewdrop.fixture.readmodel.users.GetUserByIdQuery;
import com.dewdrop.read.readmodel.query.QueryHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryHandlerUtilsTest {
    DewdropUsersReadModel readModel;
    GetUserByIdQuery query;
    DewdropUser user;
    Method method;


    @BeforeEach
    void setup() {
        readModel = new DewdropUsersReadModel();
        query = new GetUserByIdQuery(UUID.randomUUID());
        user = new DewdropUser();
        method = MethodUtils.getMatchingMethod(DewdropUsersReadModel.class, "query", GetUserByIdQuery.class);
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
    void callQueryHandler() throws ResultException {
        user.setUserId(query.getUserId());
        ReadModelUtils.updateReadModelCacheField(readModel, Map.of(user.getUserId(), user));
        try (MockedStatic<DewdropAnnotationUtils> utilities = mockStatic(DewdropAnnotationUtils.class)) {
            utilities.when(() -> DewdropAnnotationUtils.getAnnotatedMethods(any(), any(Class.class))).thenReturn(Set.of(method));
            Result<DewdropUser> result = QueryHandlerUtils.callQueryHandler(readModel, query);
            assertThat(result.get().getUserId(), is(query.getUserId()));
        }
    }

    @Test
    @DisplayName("callQueryHandler() - Given an object with a method annotated with @QueryHandler and an query, the object returned will already be a result")
    void callQueryHandler_returnResult() throws InvocationTargetException, IllegalAccessException {
        user.setUserId(query.getUserId());
        ReadModelUtils.updateReadModelCacheField(readModel, Map.of(user.getUserId(), user));
        Method spy = spy(method);
        doReturn(Result.of(user)).when(spy).invoke(any(), any());
        try (MockedStatic<DewdropAnnotationUtils> utilities = mockStatic(DewdropAnnotationUtils.class)) {
            utilities.when(() -> DewdropAnnotationUtils.getAnnotatedMethods(any(), any(Class.class))).thenReturn(Set.of(spy));

            Result<DewdropUser> result = QueryHandlerUtils.callQueryHandler(readModel, query);
            assertThat(result.isValuePresent(), is(true));
        }
    }

    @Test
    @DisplayName("callQueryHandler() - Given an object with a method annotated with @QueryHandler and an query and a null return, the object returned will be an empty result")
    void callQueryHandler_returnNull() throws InvocationTargetException, IllegalAccessException {
        user.setUserId(query.getUserId());
        ReadModelUtils.updateReadModelCacheField(readModel, Map.of(user.getUserId(), user));
        Method spy = spy(method);
        doReturn(null).when(spy).invoke(any(), any());
        try (MockedStatic<DewdropAnnotationUtils> utilities = mockStatic(DewdropAnnotationUtils.class)) {
            utilities.when(() -> DewdropAnnotationUtils.getAnnotatedMethods(any(), any(Class.class))).thenReturn(Set.of(spy));

            Result<DewdropUser> result = QueryHandlerUtils.callQueryHandler(readModel, query);
            assertThat(result.isEmpty(), is(true));
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
            utilities.when(() -> DewdropAnnotationUtils.getAnnotatedMethods(any(), any(Class.class))).thenReturn(Set.of(method));

            QueryHandlerUtils.callQueryHandler(instance, query);
            assertThat(instance.getUserId(), is(nullValue()));
        }
    }

    @Test
    @DisplayName("callQueryHandler() - Given an object without a method annotated with @QueryHandler and an query, the object will not be updated")
    void callQueryHandler_noQueryHandlerMethod() {
        NoMethodQueryHandler instance = new NoMethodQueryHandler();
        QueryHandlerUtils.callQueryHandler(instance, query);
        assertThat(instance.getUserId(), is(nullValue()));
    }


    private class NoParameterQueryHandler {
        @QueryHandler
        public void on() {}
    }

    @Data
    private class NoMethodQueryHandler {
        UUID userId;

        public void on(GetUserByIdQuery query) {
            this.userId = query.getUserId();
        }
    }
}
