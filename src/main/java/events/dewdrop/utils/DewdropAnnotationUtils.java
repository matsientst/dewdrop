package events.dewdrop.utils;

import static events.dewdrop.utils.ReflectionsConfigUtils.EXCLUDE_PACKAGES;
import static events.dewdrop.utils.ReflectionsConfigUtils.REFLECTIONS;
import static java.util.stream.Collectors.toSet;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

public class DewdropAnnotationUtils {
    private DewdropAnnotationUtils() {}

    public static Set<Field> getAnnotatedFields(Class<?> targetClass, Class<? extends Annotation> fieldAnnotation) {
        Set<Field> fields = FieldUtils.getFieldsListWithAnnotation(targetClass, fieldAnnotation).stream().collect(toSet());
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

    public static Set<Method> getAnnotatedMethods(Class<?> target, Class<? extends Annotation> annotationClass) {
        List<Method> methods = MethodUtils.getMethodsListWithAnnotation(target, annotationClass, true, true);
        if (CollectionUtils.isNotEmpty(methods)) {
            return methods.stream()
                            // We have to do this because Reflections is not excluding correctly.
                            .filter(method -> !EXCLUDE_PACKAGES.contains(method.getDeclaringClass().getPackageName())).filter(method -> {
                                if (method.getParameterTypes().length == 0) { return true; }
                                return !Objects.equals(method.getParameterTypes()[0].getSimpleName(), "Object");
                            }).collect(toSet());
        }
        return new HashSet<>();
    }

    public static Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotationClass) {
        Set<Class<?>> classes = REFLECTIONS.getTypesAnnotatedWith(annotationClass);
        return classes.stream().filter(clazz -> {
            if (EXCLUDE_PACKAGES.contains(clazz.getPackageName())) { return false; }
            return true;
        }).collect(toSet());
    }
}
