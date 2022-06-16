package com.dewdrop.aggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dewdrop.fixture.events.DewdropAccountCreated;
import com.dewdrop.structure.api.Message;
import com.dewdrop.utils.EventHandlerUtils;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class EventStateMachineTest {
    EventStateMachine eventStateMachine;
    DewdropAccountCreated event;
    EventRecorder recorder;

    @BeforeEach
    void setup() {
        eventStateMachine = spy(new AggregateRoot());
        event = new DewdropAccountCreated(UUID.randomUUID(), "Test", UUID.randomUUID());
        recorder = new EventRecorder();
        recorder.recordEvent(event);
    }

    @Test
    @DisplayName("new AggregateRoot() - Given no parameters, confirm we have a version of -1L and that recorder is initialized")
    void constructor() {
        AggregateRoot aggregateRoot = new AggregateRoot();
        assertThat(aggregateRoot.getVersion(), is(-1L));
        assertThat(aggregateRoot.recorder, is(notNullValue()));
    }

    @Test
    @DisplayName("restoreFromEvents() - Given a list of messages, confirm we have a version of 1L and that we call callEventHandler() twice")
    void restoreFromEvents() {
        List<Message> events = List.of(event, event);
        doNothing().when(eventStateMachine).callEventHandler(any(Message.class));
        eventStateMachine.restoreFromEvents(events);

        verify(eventStateMachine, times(2)).callEventHandler(any(Message.class));
        assertThat(eventStateMachine.getVersion(), is(1L));
    }

    @Test
    @DisplayName("restoreFromEvents() - Given a list of messages and an existing state in recorder, throw an IllegalStateException")
    void restoreFromEvents_existingEventsInRecorder() {
        eventStateMachine.setRecorder(recorder);

        assertThrows(IllegalStateException.class, () -> eventStateMachine.restoreFromEvents(List.of(event, event)));

        verify(eventStateMachine, times(0)).callEventHandler(any(Message.class));
    }

    @Test
    @DisplayName("updateWithEvents() - Given a list of messages and an existing state in recorder, update events")
    void updateWithEvents() {
        eventStateMachine.setRecorder(recorder);
        eventStateMachine.setVersion(0L);
        eventStateMachine.updateWithEvents(List.of(event, event), 0L);

        verify(eventStateMachine, times(2)).callEventHandler(any(Message.class));
    }

    @Test
    @DisplayName("updateWithEvents() - Given a eventStateMachine that is at -1, throw an IllegalArgumentException")
    void updateWithEvents_beginningOfState() {
        eventStateMachine.setVersion(-1L);
        assertThrows(IllegalArgumentException.class, () -> eventStateMachine.updateWithEvents(List.of(event, event), 0L));

        verify(eventStateMachine, times(0)).callEventHandler(any(Message.class));
    }

    @Test
    @DisplayName("updateWithEvents() - Given a eventStateMachine that is at 0 and an expected version of 1, throw an IllegalArgumentException")
    void updateWithEvents_mismatchedVersions() {
        eventStateMachine.setVersion(0L);
        assertThrows(IllegalArgumentException.class, () -> eventStateMachine.updateWithEvents(List.of(event, event), 1L));

        verify(eventStateMachine, times(0)).callEventHandler(any(Message.class));
    }

    @Test
    @DisplayName("takeEvents() - Given a eventStateMachine that has existing events, confirm takeEventsStarted(), takeEventsCompleted() are both called and that our version == 0 and that we have no recorded events left ")
    void takeEvents() {
        eventStateMachine.setRecorder(recorder);
        eventStateMachine.takeEvents();

        verify(eventStateMachine, times(1)).takeEventStarted();
        verify(eventStateMachine, times(1)).takeEventsCompleted();

        assertThat(eventStateMachine.getVersion(), is(0L));
        assertThat(eventStateMachine.getRecorder().hasRecordedEvents(), is(false));
    }

    @Test
    @DisplayName("raise() - Given a message, confirm that onEventRaised() is called, and that we have our event in our recorder")
    void raise() {
        eventStateMachine.raise(event);

        verify(eventStateMachine, times(1)).onEventRaised(any(Message.class));

        assertThat(eventStateMachine.getRecorder().hasRecordedEvents(), is(true));
        assertThat(eventStateMachine.getRecorder().recordedEvents().get(0), is(event));
    }

    @Test
    @DisplayName("callEventHandler() - Given a message, do we pass off a call to EventHandlerUtils.callEventHandler()")
    void callEventHandler() {
        try (MockedStatic<EventHandlerUtils> utilities = mockStatic(EventHandlerUtils.class)) {
            utilities.when(() -> EventHandlerUtils.callEventHandler(any(), any(Message.class))).then(invocationOnMock -> null);

            eventStateMachine.callEventHandler(event);
            utilities.verify(() -> EventHandlerUtils.callEventHandler(any(), any(Message.class)), times(1));
        }

    }
}
