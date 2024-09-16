package events.dewdrop.read.readmodel.stream;

import events.dewdrop.read.readmodel.annotation.AggregateStream;
import events.dewdrop.read.readmodel.annotation.CategoryStream;
import events.dewdrop.read.readmodel.annotation.EventStream;
import events.dewdrop.structure.read.Direction;
import lombok.Data;

import java.lang.annotation.Annotation;

@Data
public class StreamAnnotationDetails {
    String streamName;
    StreamType streamType;
    boolean subscribed = true;
    Direction direction = Direction.FORWARD;

    public StreamAnnotationDetails(Annotation streamAnnotation) {
        String annotationName = streamAnnotation.annotationType().getSimpleName();
        switch (annotationName) {
            case "AggregateStream":
                this.streamType = StreamType.AGGREGATE;
                AggregateStream aggregateStream = (AggregateStream) streamAnnotation;
                this.streamName = aggregateStream.name();
                this.subscribed = aggregateStream.subscribed();
                this.direction = aggregateStream.direction();
                break;
            case "EventStream":
                this.streamType = StreamType.EVENT;
                EventStream eventStream = (EventStream) streamAnnotation;
                this.streamName = eventStream.name();
                this.subscribed = eventStream.subscribed();
                this.direction = eventStream.direction();
                break;
            case "CategoryStream":
            default:
                this.streamType = StreamType.CATEGORY;
                CategoryStream categoryStream = (CategoryStream) streamAnnotation;
                this.streamName = categoryStream.name();
                this.subscribed = categoryStream.subscribed();
                this.direction = categoryStream.direction();
                break;
        }
    }

}
