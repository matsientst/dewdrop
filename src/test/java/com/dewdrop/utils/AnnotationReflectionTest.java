package com.dewdrop.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.dewdrop.aggregate.Aggregate;
import com.dewdrop.command.CommandHandler;
import com.dewdrop.fixture.automated.DewdropAccountAggregate;
import com.dewdrop.fixture.readmodel.DewdropAccountDetails;
import com.dewdrop.read.readmodel.annotation.PrimaryCacheKey;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnnotationReflectionTest {

    @BeforeEach
    void setup() {
        ReflectionsConfigUtils.init("com.dewdrop", List.of("com.dewdrop.fixture.customized"));
    }

    @Test
    @DisplayName("getAnnotatedFields() - Given an instance of an object with a field annotated with @PrimaryCacheKey and the annotation @PrimaryCacheKey, return the field marked with the annotation")
    void getAnnotatedFields() {
        DewdropAccountDetails instance = new DewdropAccountDetails();
        Set<Field> annotatedFields = AnnotationReflection.getAnnotatedFields(instance, PrimaryCacheKey.class);
        assertThat(annotatedFields.isEmpty(), is(false));
        Field[] fieldsWithAnnotation = FieldUtils.getFieldsWithAnnotation(DewdropAccountDetails.class, PrimaryCacheKey.class);
        assertThat(annotatedFields.stream().findAny().get(), is(fieldsWithAnnotation[0]));
    }

    @Test
    @DisplayName("getAnnotatedMethods() - Given a @CommandHandler annotation, return methods marked with the annotation @CommandHandler")
    void getAnnotatedMethods() {
        Set<Method> annotated = AnnotationReflection.getAnnotatedMethods(CommandHandler.class);
        assertThat(annotated.isEmpty(), is(false));
        assertThat(annotated.stream().findAny().get().isAnnotationPresent(CommandHandler.class), is(true));
    }

    @Test
    @DisplayName("getAnnotatedMethods() - Given a @TestAnnotation annotation, return an empty set")
    void getAnnotatedMethods_empty() {
        Set<Method> annotated = AnnotationReflection.getAnnotatedMethods(TestAnnotation.class);
        assertThat(annotated.isEmpty(), is(true));
    }

    @Test
    @DisplayName("getAnnotatedMethods() - Given a @CommandHandler annotation, filter out any methods who's first parameter is Object")
    void getAnnotatedMethods_skipWhenFirstParamIsObject() {
        Set<Method> annotated = AnnotationReflection.getAnnotatedMethods(CommandHandler.class);

        assertThat(annotated.stream().filter(method -> method.getDeclaringClass().getSimpleName().equals("TestCommandHandler")).findAny().isEmpty(), is(true));
    }

    @Test
    @DisplayName("getAnnotatedMethods() - Given a @CommandHandler annotation, filter out any methods who's in the EXCLUDE_PACKAGES")
    void getAnnotatedMethods_skipWhenExcluded() {
        Set<Method> annotated = AnnotationReflection.getAnnotatedMethods(CommandHandler.class);
        assertThat(annotated.stream().filter(method -> method.getDeclaringClass().equals(DewdropAccountAggregate.class)).findAny().isPresent(), is(true));

        ReflectionsConfigUtils.init("com.dewdrop", List.of("com.dewdrop.fixture.automated"));
        annotated = AnnotationReflection.getAnnotatedMethods(CommandHandler.class);
        assertThat(annotated.stream().filter(method -> method.getDeclaringClass().getSimpleName().equals("DewdropAccountAggregate")).findAny().isEmpty(), is(true));
    }

    @Test
    @DisplayName("getAnnotatedClasses() - Given an @Aggregate annotation, return classes marked with the annotation @Aggregate")
    void getAnnotatedClasses() {
        Set<Class<?>> annotated = AnnotationReflection.getAnnotatedClasses(Aggregate.class);
        assertThat(annotated.isEmpty(), is(false));
        assertThat(annotated.stream().findAny().get().isAnnotationPresent(Aggregate.class), is(true));
    }

    @Test
    @DisplayName("getAnnotatedClasses() - Given an annotation that is not used we should be returned an empty set")
    void getAnnotatedClasses_none() {
        Set<Class<?>> annotated = AnnotationReflection.getAnnotatedClasses(TestAnnotation.class);
        assertThat(annotated.isEmpty(), is(true));
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    private @interface TestAnnotation {
    }

    private class TestCommandHandler {
        @CommandHandler
        public void handle(Object object) {}
    }
}
