package org.dewdrop.aggregate;

import static java.util.Objects.requireNonNull;

import org.dewdrop.structure.api.Message;
import org.dewdrop.utils.EventHandlerUtils;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Data
@Log4j2
public abstract class EventStateMachine {
    private long version;
    protected EventRecorder recorder;

    public EventStateMachine() {
        this.version = -1;
        this.recorder = new EventRecorder();
    }

    public void restoreFromEvents(List<Message> messages) {
        requireNonNull(messages);

        if (recorder.hasRecordedEvents()) { throw new IllegalStateException("Restoring from events is not possible when an instance has recorded events."); }

        messages.forEach(message -> {
            if (version < 0) { // new aggregates have an expected version of -1 or -2
                version = 0; // got first message (zero based)
            } else {
                version++;
            }
            callEventHandler(message);
        });
    }


    public void updateWithEvents(List<Message> messages, long expectedVersion) {
        requireNonNull(messages);

        if (version < 0) { throw new IllegalArgumentException("Updating with events is not possible when an instance has no historical events"); }
        if (version != expectedVersion) { throw new IllegalArgumentException("Expected version mismatch when updating - actual version:" + version + ", expectedVersion:" + expectedVersion); }

        messages.forEach(message -> {
            version++;
            callEventHandler(message);
        });
    }


    public List<Message> takeEvents() {
        takeEventStarted();
        List<Message> records = new ArrayList<>(recorder.recordedEvents());
        recorder.reset();
        version += records.size();
        takeEventsCompleted();
        return records;
    }


    public void takeEventStarted() {
        // Not Implemented yet.
    }


    public void takeEventsCompleted() {
        // Not Implemented yet.
    }


    public void onEventRaised(Message message) {
        // Not Implemented yet.
    }

    public void raise(Message message) {
        onEventRaised(message);
        callEventHandler(message);
        recorder.recordEvent(message);
    }

    protected void callEventHandler(Message message) {
        EventHandlerUtils.callEventHandler(getTarget(), message);
    }

    protected abstract Object getTarget();
}
