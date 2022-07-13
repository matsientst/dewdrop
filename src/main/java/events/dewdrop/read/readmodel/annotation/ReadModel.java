package events.dewdrop.read.readmodel.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReadModel {
    public static final int NEVER_DESTROY = -1;
    public static final int DESTROY_IMMEDIATELY = 0;

    boolean ephemeral() default false;

    int destroyInMinutesUnused() default NEVER_DESTROY;
}
