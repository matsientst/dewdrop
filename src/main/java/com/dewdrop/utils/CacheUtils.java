package com.dewdrop.utils;

import static java.util.Objects.requireNonNull;

import com.dewdrop.aggregate.AggregateId;
import com.dewdrop.read.readmodel.annotation.CreationEvent;
import com.dewdrop.read.readmodel.annotation.ForeignCacheKey;
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
    private CacheUtils() {}
    public static boolean isCacheRoot(Message message) {
        return message.getClass().isAnnotationPresent(CreationEvent.class);
    }

    public static Field getPrimaryCacheKey(Class<?> cacheTarget) {
        requireNonNull(cacheTarget, "CacheTarget is required");

        List<Field> fields = FieldUtils.getFieldsListWithAnnotation(cacheTarget, PrimaryCacheKey.class);

        if (fields.size() > 1) { throw new IllegalArgumentException("There were more than one PrimaryCacheKeys in your cached object. There should only be one."); }

        if (fields.isEmpty()) { throw new IllegalArgumentException(String.format("Cache target:%s doesn't have a key annotated with @PrimaryCacheKey - This allows the cache to know what the primary key is", cacheTarget.getSimpleName())); }

        return fields.get(0);
    }


    public static List<Field> getForeignCacheKeys(Class<?> cacheTarget) {
        requireNonNull(cacheTarget, "CacheTarget is required");

        return FieldUtils.getFieldsListWithAnnotation(cacheTarget, ForeignCacheKey.class);
    }

    public static Optional<UUID> getCacheRootKey(Message message) {
        List<Field> fieldsListWithAnnotation = FieldUtils.getFieldsListWithAnnotation(message.getClass(), AggregateId.class);
        if (CollectionUtils.isNotEmpty(fieldsListWithAnnotation)) {
            Field field = fieldsListWithAnnotation.get(0);
            return Optional.of(DewdropReflectionUtils.readFieldValue(field, message));
        }

        return Optional.empty();
    }
}
