package com.dewdrop.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import com.dewdrop.fixture.command.DewdropCreateUserCommand;
import com.dewdrop.fixture.automated.DewdropAccountAggregate;
import com.dewdrop.fixture.command.DewdropAccountCommand;
import com.dewdrop.fixture.automated.DewdropUserAggregate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class AggregateUtilsTest {
    @BeforeEach
    void setup() {
        ReflectionsConfigUtils.init("com.dewdrop");
    }

    @Test
    @DisplayName("getAggregateRootsThatSupportCommand() - Given a valid command we should test that we can find the AggregateRoot object that uses this command")
    void getAggregateRootsThatSupportCommand() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "Test");
        assertThat(AggregateUtils.getAggregateRootsThatSupportCommand(command).get(0), is(DewdropUserAggregate.class));

    }

    @Test
    @DisplayName("getAggregateRootsThatSupportCommand() - Given an invalid command we should test that when there is no AggregateRoot that handles this command we return nothing")
    void getAggregateRootsThatSupportCommand_invalidCommand() {
        InvalidCommand instance = new InvalidCommand(UUID.randomUUID());
        assertThat(AggregateUtils.getAggregateRootsThatSupportCommand(instance).isEmpty(), is(true));
    }

    @Test
    @DisplayName("getAnnotatedAggregateRoots() - This should find all the existing annotated @AggregateRoot classes")
    void getAnnotatedAggregateRoots() {
        List<Class<?>> aggregateRoots = AggregateUtils.getAnnotatedAggregateRoots();
        int size = aggregateRoots.size();
        assertThat(aggregateRoots, hasItems(DewdropAccountAggregate.class, DewdropUserAggregate.class));

        aggregateRoots = AggregateUtils.getAnnotatedAggregateRoots();
        assertThat(aggregateRoots.size(), is(size));
    }

    @Test
    @DisplayName("getAnnotatedAggregateRoots() - This should return empty since there are no annotated @AggregateRoot classes")
    void getAnnotatedAggregateRoots_noneFound() throws ReflectiveOperationException {
        AggregateUtils.clear();
        try (MockedStatic<DewdropAnnotationUtils> utilities = mockStatic(DewdropAnnotationUtils.class)) {
            utilities.when(() -> DewdropAnnotationUtils.getAnnotatedClasses(any(Class.class))).thenReturn(new HashSet());

            List<Class<?>> aggregateRoots = AggregateUtils.getAnnotatedAggregateRoots();
            assertThat(aggregateRoots, is(empty()));
        }
    }

    private class InvalidCommand extends DewdropAccountCommand {
        public InvalidCommand(UUID accountId) {
            super(accountId);
        }
    }
}
