package events.dewdrop.read.readmodel.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CategoryStreams {
    CategoryStream[] value();
}
