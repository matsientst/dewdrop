package events.dewdrop.read.readmodel.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PrimaryCacheKey {
    // This is needed for when we have @ForeignCacheKey to know which is the primary
    Class<?> creationEvent();

    String[] alternateCacheKeys() default "";
}
