package com.dewdrop.aggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventStateMachineTest {
    @Test
    @DisplayName("new AggregateRoot() - Given no parameters, confirm we have a version of -1L and that recorder is initialized")
    void constructor() {
        AggregateRoot aggregateRoot = new AggregateRoot();
        assertThat(aggregateRoot.getVersion(), is(-1L));
        assertThat(aggregateRoot.recorder, is(notNullValue()));
    }
//    @Test
//    @DisplayName("new AggregateRoot() - Given no parameters, confirm we have a version of -1L and that recorder is initialized")
//    void constructor() {
//        AggregateRoot aggregateRoot = new AggregateRoot();
//        assertThat(aggregateRoot.getVersion(), is(-1L));
//        assertThat(aggregateRoot.recorder, is(notNullValue()));
//    }
}