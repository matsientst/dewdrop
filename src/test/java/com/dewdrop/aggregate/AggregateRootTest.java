package com.dewdrop.aggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dewdrop.fixture.automated.DewdropUserAggregate;
import com.dewdrop.fixture.command.DewdropCreateUserCommand;
import com.dewdrop.fixture.events.DewdropUserCreated;
import com.dewdrop.structure.events.CorrelationCausation;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AggregateRootTest {
    DewdropUserAggregate userAggregate;
    AggregateRoot aggregateRoot;
    TestAggregateRoot testAggregateRoot;

    @BeforeEach
    void setup() {
        userAggregate = new DewdropUserAggregate();
        aggregateRoot = new AggregateRoot(userAggregate);
        testAggregateRoot = new TestAggregateRoot();
    }

    @Test
    @DisplayName("new AggregateRoot() - Given a subclass of AggregateRoot, do we have a valid target and targetClassName")
    void constructor_empty() {
        assertThat(testAggregateRoot, is(notNullValue()));
        assertThat(testAggregateRoot.getTarget().getClass(), is(TestAggregateRoot.class));
        assertThat(testAggregateRoot.getTargetClassName(), is(TestAggregateRoot.class.getName()));
    }

    @Test
    @DisplayName("new AggregateRoot() - Given a target, do we have a valid target and targetClassName")
    void constructor() {
        assertThat(aggregateRoot, is(notNullValue()));
        assertThat(aggregateRoot.getTarget().getClass(), is(DewdropUserAggregate.class));
        assertThat(aggregateRoot.getTargetClassName(), is(DewdropUserAggregate.class.getName()));
    }

    @Test
    @DisplayName("setSource() - Given a CorrelationCausation, assign the correlationId and causationId")
    void setSource() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "test");
        command.setMessageId(UUID.randomUUID());
        aggregateRoot.setSource(command);
        assertThat(aggregateRoot.getCausationId(), is(command.getMessageId()));
        assertThat(aggregateRoot.getCorrelationId(), is(command.getCorrelationId()));
    }

    @Test
    @DisplayName("setSource() - Given a CorrelationCausation without a correlationId, throw an IllegalStateException")
    void setSource_invalidState() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "test");
        EventRecorder eventRecorder = new EventRecorder();
        eventRecorder.recordEvent(new DewdropUserCreated());
        aggregateRoot.setSource(command);
        aggregateRoot.setSource(new InvalidAggregateRoot());
        aggregateRoot.setRecorder(eventRecorder);
        assertThrows(IllegalStateException.class, () -> aggregateRoot.setSource(new InvalidAggregateRoot()));
    }

    @Test
    @DisplayName("equals() - id equals whether it's an aggregateRoot or a target")
    void equals() {
        AggregateRoot newAggregateRoot = new AggregateRoot(userAggregate);
        assertThat(aggregateRoot.equals(newAggregateRoot), is(true));

        assertThat(aggregateRoot.equals(userAggregate), is(true));
    }

    @Test
    @DisplayName("hashcode() - pass off hashcode to target")
    void hashcode() {
        AggregateRoot newAggregateRoot = new AggregateRoot(userAggregate);
        assertThat(aggregateRoot.hashCode(), is(newAggregateRoot.hashCode()));
    }

    private class TestAggregateRoot extends AggregateRoot {
    }

    private class InvalidAggregateRoot extends CorrelationCausation {

    }
}
