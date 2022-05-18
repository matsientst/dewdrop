package com.dewdrop.aggregate;

import com.dewdrop.structure.api.Command;
import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.events.CorrelationCausation;
import com.dewdrop.utils.DewdropReflectionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Data;

@Data
public class AggregateRoot extends EventStateMachine implements CorrelationCausation {
    private final List<Message> messages = new ArrayList<>();
    private Object target = null;
    private String targetClassName = null;
    public AggregateRoot() {
        super();
        this.target = this;
        this.targetClassName = this.getClass().getName();
    }
    public AggregateRoot(Object target, String targetClassName) {
        super();
        this.target = target;
        this.targetClassName = targetClassName;
    }

    private UUID correlationId;
    private UUID causationId;

    public void setSource(CorrelationCausation command) {
        if (correlationId != null && recorder.hasRecordedEvents()) {
            throw new IllegalStateException("Cannot change source unless there are no recorded events, or current source is null");
        }

        this.correlationId = command.getCorrelationId();
        this.causationId = command.getCausationId();
    }

    public EventRecorder getRecorder() {
        return recorder;
    }

    public void setRecorder(EventRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public Object getTarget() {
        return target;
    }

    public Optional<Event> handleCommand(Command command) {
        return DewdropReflectionUtils.callMethod(getTarget(), "handle", command);
    }

}
