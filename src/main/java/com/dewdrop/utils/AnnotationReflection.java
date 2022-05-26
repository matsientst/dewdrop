package com.dewdrop.utils;

import static com.dewdrop.utils.ReflectionsConfigUtils.EXCLUDE_PACKAGES;
import static com.dewdrop.utils.ReflectionsConfigUtils.REFLECTIONS;
import static java.util.stream.Collectors.toSet;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

public class AnnotationReflection {
    private AnnotationReflection() {}

    public static Set<Field> getAnnotatedFields(Object target, Class<? extends Annotation> fieldAnnotation) {
        Set<Field> fields = FieldUtils.getFieldsListWithAnnotation(target.getClass(), fieldAnnotation).stream().collect(toSet());
        return fields;
    }

    public static Set<Method> getAnnotatedMethods(Class<? extends Annotation> annotationClass) {
        Set<Method> methods = REFLECTIONS.getMethodsAnnotatedWith(annotationClass);
        if (CollectionUtils.isNotEmpty(methods)) {
            methods = methods.stream()
                            // We have to do this because Reflections is not excluding correctly.
                            .filter(method -> !EXCLUDE_PACKAGES.contains(method.getDeclaringClass().getPackageName())).filter(method -> !Objects.equals(method.getParameterTypes()[0].getSimpleName(), "Object")).collect(toSet());
        }
        return methods;
    }

    public static Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotationClass) {
        Set<Class<?>> classes = REFLECTIONS.getTypesAnnotatedWith(annotationClass);
        return classes.stream().collect(toSet());
    }
}
