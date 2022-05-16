package com.dewdropper.utils;

import static java.util.stream.Collectors.toSet;
import static org.reflections.scanners.Scanners.FieldsAnnotated;
import static org.reflections.scanners.Scanners.MethodsAnnotated;
import static org.reflections.scanners.Scanners.TypesAnnotated;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

@Log4j2
public class ReflectionUtils {
    private static Reflections fieldsReflections = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage("com.dewdropper"))
        .setScanners(FieldsAnnotated));
    private static Reflections methodsReflections = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage("com.dewdropper"))
        .setScanners(MethodsAnnotated));
    private static Reflections typesAnnotated = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage("com.dewdropper"))
        .setScanners(TypesAnnotated));

    public static Set<Field> getAnnotatedFields(Object target, Class<? extends Annotation> fieldAnnotation) {
        Set<Field> fields = FieldUtils.getFieldsListWithAnnotation(target.getClass(), fieldAnnotation).stream().collect(toSet());
        return fields;
    }

    public static Set<Method> getAnnotatedMethods(Class<? extends Annotation> annotationClass) {
        Set<Method> methods = methodsReflections.getMethodsAnnotatedWith(annotationClass);
        if (CollectionUtils.isNotEmpty(methods)) {
            methods = methods.stream()
                .filter(method -> !Objects.equals(method.getParameterTypes()[0].getSimpleName(), "Object"))
                .collect(toSet());
        }
        return methods;
    }

    public static Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotationClass) {
        Set<Class<?>> classes = typesAnnotated.getTypesAnnotatedWith(annotationClass);
        if (CollectionUtils.isNotEmpty(classes)) {
            return classes.stream()
                .collect(toSet());
        }
        return new HashSet<>();
    }

    public static <T> Optional<T> callMethod(Object object, String method, Object... args) {
        try {
            T result = (T) MethodUtils.invokeMethod(object, true, method, args);
            if (result != null) {
                return Optional.of(result);
            }
        } catch (IllegalArgumentException | InvocationTargetException e) {
            log.error("Unable to invoke {} on {} with args:{} - message: {}", method, object.getClass()
                .getSimpleName(), args, e.getMessage(), e);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            log.error("We were unable to find the method:{}() on {} with args:{} - message: {}", method, object.getClass()
                .getSimpleName(), args, e.getMessage(), e);
        }
        return Optional.empty();
    }

}
