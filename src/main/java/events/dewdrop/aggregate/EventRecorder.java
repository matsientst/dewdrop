package events.dewdrop.aggregate;

import static java.util.Objects.requireNonNull;

import events.dewdrop.structure.api.Message;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EventRecorder implements Serializable {
    private List<Message> recorded;

    public EventRecorder() {
        recorded = new ArrayList<>();
    }

    public boolean hasRecordedEvents() {
        return !recorded.isEmpty();
    }

    public void recordEvent(Message message) {
        requireNonNull(message);

        recorded.add(message);
    }

    public void reset() {
        recorded.clear();
    }


    public List<Message> recordedEvents() {
        return recorded;
    }
}
