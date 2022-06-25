package com.dewdrop.read.readmodel.annotation;

import com.dewdrop.read.readmodel.stream.StreamType;
import com.dewdrop.structure.read.Direction;
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
