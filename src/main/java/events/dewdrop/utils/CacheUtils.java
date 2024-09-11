package events.dewdrop.utils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import events.dewdrop.aggregate.annotation.AggregateId;
import events.dewdrop.read.readmodel.annotation.CreationEvent;
import events.dewdrop.read.readmodel.annotation.ForeignCacheKey;
import events.dewdrop.read.readmodel.annotation.PrimaryCacheKey;
import events.dewdrop.structure.api.Event;
import events.dewdrop.structure.api.Message;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

@Log4j2
public class CacheUtils {
    private CacheUtils() {}

    public static boolean isCacheRoot(Message message) {
        return message.getClass().isAnnotationPresent(CreationEvent.class);
    }

    public static List<String> getPrimaryCacheKeys(Class<?> cacheTarget) {
        requireNonNull(cacheTarget, "CacheTarget is required");

        Set<String> primaryCacheKeys = new HashSet<>();
        final Set<Field> fields = FieldUtils.getFieldsListWithAnnotation(cacheTarget, PrimaryCacheKey.class).stream().collect(toSet());

        if (fields.size() > 1) { throw new IllegalArgumentException("There were more than one PrimaryCacheKeys in your cached object. There should only be one."); }

        if (fields.isEmpty()) { throw new IllegalArgumentException(String.format("Cache target:%s doesn't have a key annotated with @PrimaryCacheKey - This allows the cache to know what the primary key is", cacheTarget.getSimpleName())); }

        Field field = fields.iterator().next();
        PrimaryCacheKey annotation = field.getAnnotation(PrimaryCacheKey.class);
        primaryCacheKeys.add(field.getName());
        Arrays.asList(annotation.alternateCacheKeys()).stream().filter(key -> StringUtils.isNotEmpty(key)).forEach(key -> primaryCacheKeys.add(key));

        return primaryCacheKeys.stream().collect(toList());
    }

    public static Class<? extends Event> getCreationEventClass(Class<?> cacheTarget) {
        final Set<Field> fields = FieldUtils.getFieldsListWithAnnotation(cacheTarget, PrimaryCacheKey.class).stream().collect(toSet());
        Field field = fields.iterator().next();
        Class<?> clazz = field.getDeclaredAnnotation(PrimaryCacheKey.class).creationEvent();
        if (Event.class.isAssignableFrom(clazz)) {
            Class<? extends Event> classWithAggregateId = getClassWithAggregateId(clazz);
            return classWithAggregateId;
        }
        throw new IllegalStateException("The CreationEvent class[" + clazz + "] is not a type of Event");
    }

    public static List<String> getForeignCacheKeys(Class<?> cacheTarget) {
        requireNonNull(cacheTarget, "CacheTarget is required");

        List<Field> fields = FieldUtils.getFieldsListWithAnnotation(cacheTarget, ForeignCacheKey.class);
        return fields.stream().map(field -> {
            String keyName = field.getAnnotation(ForeignCacheKey.class).eventKeyField();
            return Optional.ofNullable(keyName).orElse(field.getName());
        }).collect(toList());
    }

    public static List<Field> getForeignCacheKeyFields(Class<?> cacheTarget) {
        requireNonNull(cacheTarget, "CacheTarget is required");

        return FieldUtils.getFieldsListWithAnnotation(cacheTarget, ForeignCacheKey.class);
    }

    public static Optional<UUID> getTargetForeignKeyValue(Object dto, Field field) {
        requireNonNull(dto, "DTO is required");
        requireNonNull(field, "Field is required");

        return DewdropReflectionUtils.readFieldValue(field, dto);
    }

    public static Optional<UUID> getForeignCacheEventKeyValue(Message message, Field field) {
        requireNonNull(message, "Message is required");
        requireNonNull(field, "Field is required");

        String keyName = field.getAnnotation(ForeignCacheKey.class).eventKeyField();
        return DewdropReflectionUtils.readFieldValue(message, keyName);
    }

    public static Optional<UUID> getCacheRootKey(Message message) {
        List<Field> fieldsListWithAnnotation = FieldUtils.getFieldsListWithAnnotation(message.getClass(), AggregateId.class);
        if (CollectionUtils.isNotEmpty(fieldsListWithAnnotation)) {
            Field field = fieldsListWithAnnotation.get(0);
            return Optional.of(DewdropReflectionUtils.readFieldValue(field, message));
        }

        return Optional.empty();
    }

    public static Class<? extends Event> getClassWithAggregateId(Class<?> clazz) {
        List<Field> fieldsListWithAnnotation = FieldUtils.getFieldsListWithAnnotation(clazz, AggregateId.class);
        if (CollectionUtils.isNotEmpty(fieldsListWithAnnotation)) {
            Field field = fieldsListWithAnnotation.get(0);
            Class<?> declaringClass = field.getDeclaringClass();
            if (Event.class.isAssignableFrom(declaringClass)) { return (Class<? extends Event>) declaringClass; }
            throw new IllegalStateException("The CreationEvent class[" + clazz + "] is not a type of Event");
        }
        throw new IllegalStateException("No Fields in class or super annotated with @AggregateId");
    }
}
