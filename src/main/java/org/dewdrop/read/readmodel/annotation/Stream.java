package org.dewdrop.read.readmodel.annotation;

import org.dewdrop.read.readmodel.stream.StreamType;
import org.dewdrop.structure.read.Direction;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Streams.class)
public @interface Stream {
    StreamType streamType() default StreamType.CATEGORY;

    String name();

    Direction direction() default Direction.FORWARD;

    boolean subscribed() default true;
}
