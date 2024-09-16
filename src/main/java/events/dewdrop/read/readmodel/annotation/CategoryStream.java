package events.dewdrop.read.readmodel.annotation;

import events.dewdrop.structure.read.Direction;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(value = CategoryStreams.class)
public @interface CategoryStream {
    String name();

    Direction direction() default Direction.FORWARD;

    boolean subscribed() default true;
}
