package org.dewdrop.aggregate;

import org.dewdrop.structure.api.Message;
import org.dewdrop.structure.events.CorrelationCausation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public class AggregateRoot extends EventStateMachine {
    private final List<Message> messages = new ArrayList<>();
    private Object target = null;
    private String targetClassName = null;

    public AggregateRoot() {
        super();
        this.target = this;
        this.targetClassName = this.getClass().getName();
    }

    public AggregateRoot(Object target) {
        super();
        this.target = target;
        this.targetClassName = target.getClass().getName();
    }

    private UUID correlationId;
    private UUID causationId;

    public void setSource(CorrelationCausation command) {
        if (correlationId != null && recorder.hasRecordedEvents()) { throw new IllegalStateException("Cannot change source unless there are no recorded events, or current source is null"); }

        this.correlationId = command.getCorrelationId();
        this.causationId = command.getMessageId();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AggregateRoot) { return getTarget().equals(((AggregateRoot) o).getTarget()); }
        return getTarget().equals(o);
    }

    @Override
    public int hashCode() {
        return getTarget().hashCode();
    }
}
