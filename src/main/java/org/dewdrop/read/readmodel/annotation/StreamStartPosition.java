package org.dewdrop.read.readmodel.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.dewdrop.read.readmodel.stream.StreamType;

@Target({METHOD})
@Retention(RUNTIME)
public @interface StreamStartPosition {
    String name();

    StreamType streamType() default StreamType.CATEGORY;
}
