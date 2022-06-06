package com.dewdrop.aggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.dewdrop.fixture.events.DewdropUserCreated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventRecorderTest {
    EventRecorder eventRecorder;

    @BeforeEach
    void setup() {
        this.eventRecorder = new EventRecorder();
    }

    @Test
    @DisplayName("new EventRecorder() - Given a valid construction, we have an initialized recordedEvents field")
    void constructor() {
        assertThat(eventRecorder.recordedEvents().isEmpty(), is(true));
        assertThat(eventRecorder.hasRecordedEvents(), is(false));
    }

    @Test
    @DisplayName("recordEvent() - Given an event, after we call recordEvent() we can retrieve it from getRecorded()")
    void recordEvent() {
        DewdropUserCreated event = new DewdropUserCreated();
        eventRecorder.recordEvent(event);
        assertThat(eventRecorder.hasRecordedEvents(), is(true));
        assertThat(eventRecorder.recordedEvents().get(0), is(event));
    }

    @Test
    @DisplayName("reset() - on reset, we will no longer have any recordedEvents")
    void reset() {
        recordEvent();
        assertThat(eventRecorder.hasRecordedEvents(), is(true));

        eventRecorder.reset();
        assertThat(eventRecorder.hasRecordedEvents(), is(false));
    }

    @Test
    @DisplayName("recordedEvents() - return a collection of recordedEvents")
    void recordedEvents() {
        recordEvent();
        assertThat(eventRecorder.recordedEvents().size(), is(1));
    }
}
