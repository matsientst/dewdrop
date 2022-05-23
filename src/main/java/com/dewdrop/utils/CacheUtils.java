package com.dewdrop.utils;

import static java.util.Objects.requireNonNull;

import com.dewdrop.aggregate.AggregateId;
import com.dewdrop.read.readmodel.annotation.AlternateCacheKey;
import com.dewdrop.read.readmodel.annotation.CreationEvent;
import com.dewdrop.read.readmodel.annotation.PrimaryCacheKey;
import com.dewdrop.structure.api.Message;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

@Log4j2
public class CacheUtils {
    public static boolean isCacheRoot(Message message) {
        return message.getClass()
            .isAnnotationPresent(CreationEvent.class);
    }

    public static Field getPrimaryCacheKey(Class<?> cacheTarget) {
        requireNonNull(cacheTarget, "CacheTarget is required");

        List<Field> fields = FieldUtils.getFieldsListWithAnnotation(cacheTarget, PrimaryCacheKey.class);
        if(fields.size() > 1) {
            log.error("There were more than one PrimaryCacheKeys in your cached object. This should only be one.");
        }

        return fields.get(0);
    }


    public static List<Field> getAlternateCacheKeys(Class<?> cacheTarget) {
        requireNonNull(cacheTarget, "CacheTarget is required");

        return FieldUtils.getFieldsListWithAnnotation(cacheTarget, AlternateCacheKey.class);
    }

    public static Optional<UUID> getCacheRootKey(Message message) {
        List<Field> fieldsListWithAnnotation = FieldUtils.getFieldsListWithAnnotation(message.getClass(), AggregateId.class);
        if (CollectionUtils.isNotEmpty(fieldsListWithAnnotation)) {
            Field field = fieldsListWithAnnotation.get(0);
            field.setAccessible(true);
            try {
                return Optional.of((UUID) field.get(message));
            } catch (IllegalAccessException | NullPointerException e) {
                log.error("Message was unable to access the cacheRootKey:{}", message, e);
            }
        }

        return Optional.empty();
    }
}
