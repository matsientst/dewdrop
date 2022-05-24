package com.dewdrop.utils;

import com.dewdrop.aggregate.AggregateId;
import com.dewdrop.aggregate.AggregateRoot;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

@Log4j2
public class AggregateIdUtils {
    public static Optional<UUID> getAggregateId(AggregateRoot aggregateRoot) {
        return getAggregateId(aggregateRoot.getTarget());
    }

    public static Optional<UUID> getAggregateId(Object target) {
        Set<Field> annotatedFields = AnnotationReflection.getAnnotatedFields(target, AggregateId.class);
        Class<?> superclass = target.getClass().getSuperclass();

        while (CollectionUtils.isEmpty(annotatedFields)) {
            annotatedFields = AnnotationReflection.getAnnotatedFields(superclass, AggregateId.class);
            superclass = superclass.getSuperclass();
            if (superclass.getSimpleName().equals("Object")) {
                break;
            }
        }

        if (CollectionUtils.isEmpty(annotatedFields)) {
            log.error("No field was marked @AggregateId on {}", target.getClass().getSimpleName());
            throw new IllegalArgumentException("Missing @AggregateId annotation");
        }

        if (annotatedFields.size() > 1) {
            log.error("Too many @AggregateId annotations were found on {}", target.getClass().getSimpleName());
            throw new IllegalArgumentException("Too many @AggregateId annotations");
        }

        try {
            Field field = new ArrayList<>(annotatedFields).get(0);
            field.setAccessible(true);
            UUID uuid = (UUID) field.get(target);
            return Optional.ofNullable(uuid);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
