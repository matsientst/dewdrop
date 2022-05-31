package com.dewdrop.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.dewdrop.fixture.command.DewdropCreateUserCommand;
import com.dewdrop.fixture.automated.DewdropUserAggregate;
import com.dewdrop.fixture.events.DewdropUserCreated;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class DewdropReflectionUtilsTest {
    Field field = FieldUtils.getField(DewdropCreateUserCommand.class, "userId", true);

    @Test
    @DisplayName("hasField() - Given an object does it have the field associated with it")
    void hasField() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "test");
        assertThat(DewdropReflectionUtils.hasField(command, "userId"), is(true));
        assertThat(DewdropReflectionUtils.hasField(command, "accountId"), is(false));
    }

    @Test
    @DisplayName("readFieldValue() - Given an object can we get the field associated with it")
    void readFieldValue() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "test");
        assertThat(DewdropReflectionUtils.readFieldValue(command, "userId").get(), is(command.getUserId()));
    }

    @Test
    @DisplayName("readFieldValue() - Given an object can we get the field associated with it when it's null")
    void readFieldValue_noValue() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "test");
        assertThat(DewdropReflectionUtils.readFieldValue(command, "accountId").isEmpty(), is(true));
    }

    @Test
    @DisplayName("readFieldValue() - Given a valid field, we are able to retrieve it's value")
    void readFieldValue_passField() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "test");
        assertThat(DewdropReflectionUtils.readFieldValue(field, command), is(command.getUserId()));
    }

    @Test
    @DisplayName("readFieldValue() - Given a valid field, we get null when there is available field")
    void readFieldValue_passField_problemField() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "test");
        try (MockedStatic<FieldUtils> utilities = mockStatic(FieldUtils.class)) {
            utilities.when(() -> FieldUtils.readField(any(Field.class), any(Object.class), anyBoolean())).thenThrow(new IllegalAccessException());

            assertThat(DewdropReflectionUtils.readFieldValue(field, command), is(nullValue()));
        }
    }

    @Test
    @DisplayName("callMethod() - Given a valid argument, are we able to call a method on a target object and validate the response")
    void callMethod() {
        DewdropUserAggregate aggregate = new DewdropUserAggregate();
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "test");
        Optional<DewdropUserCreated> result = DewdropReflectionUtils.callMethod(aggregate, "handle", command);

        DewdropUserCreated response = result.get();
        assertThat(response, is(notNullValue()));
        assertThat(response.getUsername(), is(command.getUsername()));
        assertThat(response.getUserId(), is(command.getUserId()));
    }

    @Test
    @DisplayName("callMethod() - Given a valid argument, are we able to call a method on a target object and get a void response")
    void callMethod_nullReturn() {
        DewdropUserAggregate aggregate = new DewdropUserAggregate();
        DewdropUserCreated created = new DewdropUserCreated(UUID.randomUUID(), "test");
        Optional<DewdropUserCreated> result = DewdropReflectionUtils.callMethod(aggregate, "on", created);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    @DisplayName("callMethod() - Given an invalid argument, are we able to call throw an IllegalArgumentException")
    void callMethod_IllegalArgumentException() {
        DewdropUserAggregate aggregate = new DewdropUserAggregate();
        DewdropUserCreated created = new DewdropUserCreated(UUID.randomUUID(), "test");

        try (MockedStatic<MethodUtils> utilities = mockStatic(MethodUtils.class)) {
            utilities.when(() -> MethodUtils.invokeMethod(any(DewdropUserAggregate.class), anyBoolean(), anyString(), any())).thenThrow(new IllegalArgumentException());

            assertThat(DewdropReflectionUtils.callMethod(aggregate, "on", created).isEmpty(), is(true));
        }
    }

    @Test
    @DisplayName("callMethod() - Given an invalid argument, are we able to call throw an InvocationTargetException")
    void callMethod_InvocationTargetException() {
        DewdropUserAggregate aggregate = new DewdropUserAggregate();
        DewdropUserCreated created = new DewdropUserCreated(UUID.randomUUID(), "test");

        try (MockedStatic<MethodUtils> utilities = mockStatic(MethodUtils.class)) {
            utilities.when(() -> MethodUtils.invokeMethod(any(), anyBoolean(), anyString(), any())).thenThrow(mock(InvocationTargetException.class));

            assertThat(DewdropReflectionUtils.callMethod(aggregate, "on", created).isEmpty(), is(true));
        }
    }

    @Test
    @DisplayName("callMethod() - Given an invalid argument, are we able to call throw an NoSuchMethodException")
    void callMethod_NoSuchMethodException() {
        DewdropUserAggregate aggregate = new DewdropUserAggregate();
        DewdropUserCreated created = new DewdropUserCreated(UUID.randomUUID(), "test");

        try (MockedStatic<MethodUtils> utilities = mockStatic(MethodUtils.class)) {
            utilities.when(() -> MethodUtils.invokeMethod(any(), anyBoolean(), anyString(), any())).thenThrow(new NoSuchMethodException());

            assertThat(DewdropReflectionUtils.callMethod(aggregate, "on", created).isEmpty(), is(true));
        }
    }

    @Test
    @DisplayName("callMethod() - Given an invalid argument, are we able to call throw an IllegalAccessException")
    void callMethod_IllegalAccessException() {
        DewdropUserAggregate aggregate = new DewdropUserAggregate();
        DewdropUserCreated created = new DewdropUserCreated(UUID.randomUUID(), "test");

        try (MockedStatic<MethodUtils> utilities = mockStatic(MethodUtils.class)) {
            utilities.when(() -> MethodUtils.invokeMethod(any(), anyBoolean(), anyString(), any())).thenThrow(new IllegalAccessException());

            assertThat(DewdropReflectionUtils.callMethod(aggregate, "on", created).isEmpty(), is(true));
        }
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
