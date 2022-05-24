package com.dewdrop.utils;

import static java.util.stream.Collectors.toSet;
import static org.reflections.scanners.Scanners.FieldsAnnotated;
import static org.reflections.scanners.Scanners.MethodsAnnotated;
import static org.reflections.scanners.Scanners.TypesAnnotated;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

public class AnnotationReflection {
    public static Set<Field> getAnnotatedFields(Object target, Class<? extends Annotation> fieldAnnotation) {
        Set<Field> fields = FieldUtils.getFieldsListWithAnnotation(target.getClass(), fieldAnnotation).stream().collect(toSet());
        return fields;
    }

    public static Set<Method> getAnnotatedMethods(Class<? extends Annotation> annotationClass) {
        Set<Method> methods = ReflectionsConfigUtils.METHODS_REFLECTIONS.getMethodsAnnotatedWith(annotationClass);
        if (CollectionUtils.isNotEmpty(methods)) {
            methods = methods.stream().filter(method -> !Objects.equals(method.getParameterTypes()[0].getSimpleName(), "Object")).collect(toSet());
        }
        return methods;
    }

    public static Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotationClass) {
        Set<Class<?>> classes = ReflectionsConfigUtils.TYPES_REFLECTIONS.getTypesAnnotatedWith(annotationClass);
        if (CollectionUtils.isNotEmpty(classes)) { return classes.stream().collect(toSet()); }
        return new HashSet<>();
    }
}
