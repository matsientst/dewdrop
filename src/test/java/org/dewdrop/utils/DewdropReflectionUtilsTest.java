package org.dewdrop.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.dewdrop.api.result.Result;
import org.dewdrop.api.result.ResultException;
import org.dewdrop.fixture.automated.DewdropUserAggregate;
import org.dewdrop.fixture.command.DewdropCreateUserCommand;
import org.dewdrop.fixture.events.DewdropUserCreated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@Log4j2
class DewdropReflectionUtilsTest {
    DewdropUserAggregate aggregate;
    DewdropCreateUserCommand command;
    DewdropUserCreated created;
    Field field;
    Method method;

    @BeforeEach
    void setup() {
        ReflectionsConfigUtils.init("org.dewdrop");
        aggregate = new DewdropUserAggregate();
        command = new DewdropCreateUserCommand(UUID.randomUUID(), "test");
        created = new DewdropUserCreated(UUID.randomUUID(), "test");
        field = FieldUtils.getField(DewdropCreateUserCommand.class, "userId", true);
        method = mock(Method.class);
    }

    @Test
    @DisplayName("hasField() - Given an object does it have the field associated with it")
    void hasField() {
        assertThat(DewdropReflectionUtils.hasField(command, "userId"), is(true));
        assertThat(DewdropReflectionUtils.hasField(command, "accountId"), is(false));
    }

    @Test
    @DisplayName("readFieldValue() - Given an object can we get the field associated with it")
    void readFieldValue() {
        assertThat(DewdropReflectionUtils.readFieldValue(command, "userId").get(), is(command.getUserId()));
    }

    @Test
    @DisplayName("readFieldValue() - Given an object can we get the field associated with it when it's null")
    void readFieldValue_noValue() {
        assertThat(DewdropReflectionUtils.readFieldValue(command, "accountId").isEmpty(), is(true));
    }

    @Test
    @DisplayName("readFieldValue() - Given a valid field, we are able to retrieve it's value")
    void readFieldValue_passField() {
        assertThat(DewdropReflectionUtils.readFieldValue(field, command), is(command.getUserId()));
    }

    @Test
    @DisplayName("readFieldValue() - Given a valid field, we get null when there is no available field")
    void readFieldValue_passField_problemField() {
        try (MockedStatic<FieldUtils> utilities = mockStatic(FieldUtils.class)) {
            utilities.when(() -> FieldUtils.readField(any(Field.class), any(DewdropCreateUserCommand.class), anyBoolean())).thenThrow(new IllegalAccessException());

            assertThat(DewdropReflectionUtils.readFieldValue(field, command), is(nullValue()));
        }
    }

    @Test
    @DisplayName("callMethod() - Given a valid argument, are we able to call a method on a target object and validate the response")
    void callMethod() throws ResultException {
        Result<DewdropUserCreated> result = DewdropReflectionUtils.callMethod(aggregate, "handle", command);

        DewdropUserCreated response = result.get();
        assertThat(response, is(notNullValue()));
        assertThat(response.getUsername(), is(command.getUsername()));
        assertThat(response.getUserId(), is(command.getUserId()));
    }

    @Test
    @DisplayName("callMethod() - Given a valid argument, are we able to call a method on a target object and get a void response")
    void callMethod_nullReturn() {
        Result<DewdropUserCreated> result = DewdropReflectionUtils.callMethod(aggregate, "on", created);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    @DisplayName("callMethod() - Given an invalid argument, are we able to call throw an IllegalArgumentException")
    void callMethod_IllegalArgumentException() {
        try (MockedStatic<MethodUtils> utilities = mockStatic(MethodUtils.class)) {
            utilities.when(() -> MethodUtils.invokeMethod(any(DewdropUserAggregate.class), anyBoolean(), anyString(), any())).thenThrow(new IllegalArgumentException());

            assertThat(DewdropReflectionUtils.callMethod(aggregate, "on", created).isExceptionPresent(), is(true));
        }
    }

    @Test
    @DisplayName("callMethod() - Given an invalid argument, are we able to call throw an InvocationTargetException")
    void callMethod_InvocationTargetException() {
        try (MockedStatic<MethodUtils> utilities = mockStatic(MethodUtils.class)) {
            utilities.when(() -> MethodUtils.invokeMethod(any(), anyBoolean(), anyString(), any())).thenThrow(mock(InvocationTargetException.class));

            assertThat(DewdropReflectionUtils.callMethod(aggregate, "on", created).isExceptionPresent(), is(true));
        }
    }

    @Test
    @DisplayName("callMethod() - Given an invalid argument, are we able to call throw an NoSuchMethodException")
    void callMethod_NoSuchMethodException() {
        try (MockedStatic<MethodUtils> utilities = mockStatic(MethodUtils.class)) {
            utilities.when(() -> MethodUtils.invokeMethod(any(), anyBoolean(), anyString(), any())).thenThrow(new NoSuchMethodException());

            assertThat(DewdropReflectionUtils.callMethod(aggregate, "on", created).isExceptionPresent(), is(true));
        }
    }

    @Test
    @DisplayName("callMethod() - Given an invalid argument, are we able to call throw an IllegalAccessException")
    void callMethod_IllegalAccessException() {
        try (MockedStatic<MethodUtils> utilities = mockStatic(MethodUtils.class)) {
            utilities.when(() -> MethodUtils.invokeMethod(any(), anyBoolean(), anyString(), any())).thenThrow(new IllegalAccessException());

            assertThat(DewdropReflectionUtils.callMethod(aggregate, "on", created).isExceptionPresent(), is(true));
        }
    }

    @Test
    @DisplayName("callMethod() - Given an object a method and an argument, when we call callMethod(), we call invoke on that method")
    void callMethod_methodParameter() throws ResultException, InvocationTargetException, IllegalAccessException {
        doReturn(new DewdropUserCreated()).when(method).invoke(any(), any());

        Result<DewdropUserCreated> result = DewdropReflectionUtils.callMethod(aggregate, method, command);

        DewdropUserCreated response = result.get();
        assertThat(response, is(notNullValue()));
    }

    @Test
    @DisplayName("callMethod() - Given an object a method and an argument, when we call callMethod() and have a null response, then the method returns Result.empty()")
    void callMethod_methodParameter_nullReturn() throws InvocationTargetException, IllegalAccessException {
        doReturn(null).when(method).invoke(any(), any());
        Result<DewdropUserCreated> result = DewdropReflectionUtils.callMethod(aggregate, method, created);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    @DisplayName("callMethod() - Given an object a method and an argument, when we call callMethod() and have an IllegalArgumentException, then the method returns Result.of(IllegalArgumentException)")
    void callMethod_methodParameter_IllegalArgumentException() throws InvocationTargetException, IllegalAccessException {
        doThrow(new IllegalArgumentException()).when(method).invoke(any(), any());

        Result<DewdropUserCreated> result = DewdropReflectionUtils.callMethod(aggregate, method, created);
        assertThat(result.isExceptionPresent(), is(true));
        assertThat(result.getException().getClass(), is(IllegalArgumentException.class));
    }

    @Test
    @DisplayName("callMethod() -  Given an object a method and an argument, when we call callMethod() and have an InvocationTargetException, then the method returns Result.of(InvocationTargetException)")
    void callMethod_methodParameter_InvocationTargetException() throws InvocationTargetException, IllegalAccessException {
        doThrow(new InvocationTargetException(new RuntimeException())).when(method).invoke(any(), any());

        Result<DewdropUserCreated> result = DewdropReflectionUtils.callMethod(aggregate, method, created);
        assertThat(result.isExceptionPresent(), is(true));
        assertThat(result.getException().getClass(), is(InvocationTargetException.class));

    }

    @Test
    @DisplayName("callMethod() -Given an object a method and an argument, when we call callMethod() and have an IllegalAccessException, then the method returns Result.of(IllegalAccessException)")
    void callMethod_methodParameter_IllegalAccessException() throws InvocationTargetException, IllegalAccessException {
        doThrow(new IllegalAccessException()).when(method).invoke(any(), any());

        Result<DewdropUserCreated> result = DewdropReflectionUtils.callMethod(aggregate, method, created);
        assertThat(result.isExceptionPresent(), is(true));
        assertThat(result.getException().getClass(), is(IllegalAccessException.class));
    }

    @Test
    @DisplayName("createInstance() - Given a class with an empty constructor, are we able to construct it")
    void createInstance() {
        Optional<DewdropUserAggregate> instance = DewdropReflectionUtils.createInstance(DewdropUserAggregate.class);
        assertThat(instance.get().getClass(), is(DewdropUserAggregate.class));
    }

    @Test
    @DisplayName("createInstance() - Given a class that throws an InstantiationException on creation, do we receive an empty Optional")
    void createInstance_InstantiationException() {
        try (MockedStatic<ConstructorUtils> utilities = mockStatic(ConstructorUtils.class)) {
            utilities.when(() -> ConstructorUtils.invokeConstructor(any(Class.class))).thenThrow(new InstantiationException());

            Optional<DewdropUserAggregate> instance = DewdropReflectionUtils.createInstance(DewdropUserAggregate.class);
            assertThat(instance.isEmpty(), is(true));
        }

    }

    @Test
    @DisplayName("createInstance() - Given a class that throws an IllegalArgumentException on creation, do we receive an empty Optional")
    void createInstance_IllegalArgumentException() {
        try (MockedStatic<ConstructorUtils> utilities = mockStatic(ConstructorUtils.class)) {
            utilities.when(() -> ConstructorUtils.invokeConstructor(any(Class.class))).thenThrow(new IllegalArgumentException());

            Optional<DewdropUserAggregate> instance = DewdropReflectionUtils.createInstance(DewdropUserAggregate.class);
            assertThat(instance.isEmpty(), is(true));
        }

    }

    @Test
    @DisplayName("createInstance() - Given a class that throws an InvocationTargetException on creation, do we receive an empty Optional")
    void createInstance_InvocationTargetException() {
        try (MockedStatic<ConstructorUtils> utilities = mockStatic(ConstructorUtils.class)) {
            utilities.when(() -> ConstructorUtils.invokeConstructor(any(Class.class))).thenThrow(mock(InvocationTargetException.class));

            Optional<DewdropUserAggregate> instance = DewdropReflectionUtils.createInstance(DewdropUserAggregate.class);
            assertThat(instance.isEmpty(), is(true));
        }

    }

    @Test
    @DisplayName("createInstance() - Given a class that throws an SecurityException on creation, do we receive an empty Optional")
    void createInstance_SecurityException() {
        try (MockedStatic<ConstructorUtils> utilities = mockStatic(ConstructorUtils.class)) {
            utilities.when(() -> ConstructorUtils.invokeConstructor(any(Class.class))).thenThrow(new SecurityException());

            Optional<DewdropUserAggregate> instance = DewdropReflectionUtils.createInstance(DewdropUserAggregate.class);
            assertThat(instance.isEmpty(), is(true));
        }

    }

    @Test
    @DisplayName("createInstance() - Given a class that throws an NoSuchMethodException on creation, do we receive an empty Optional")
    void createInstance_NoSuchMethodException() {
        try (MockedStatic<ConstructorUtils> utilities = mockStatic(ConstructorUtils.class)) {
            utilities.when(() -> ConstructorUtils.invokeConstructor(any(Class.class))).thenThrow(new NoSuchMethodException());

            Optional<DewdropUserAggregate> instance = DewdropReflectionUtils.createInstance(DewdropUserAggregate.class);
            assertThat(instance.isEmpty(), is(true));
        }

    }

    @Test
    @DisplayName("createInstance() - Given a class that throws an IllegalAccessException on creation, do we receive an empty Optional")
    void createInstance_IllegalAccessException() {
        try (MockedStatic<ConstructorUtils> utilities = mockStatic(ConstructorUtils.class)) {
            utilities.when(() -> ConstructorUtils.invokeConstructor(any(Class.class))).thenThrow(new IllegalAccessException());

            Optional<DewdropUserAggregate> instance = DewdropReflectionUtils.createInstance(DewdropUserAggregate.class);
            assertThat(instance.isEmpty(), is(true));
        }

    }
}
