package events.dewdrop.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mockStatic;

import events.dewdrop.aggregate.AggregateRoot;
import events.dewdrop.aggregate.annotation.AggregateId;
import events.dewdrop.command.CommandHandler;
import events.dewdrop.fixture.automated.DewdropUserAggregate;
import events.dewdrop.fixture.command.DewdropCreateUserCommand;
import events.dewdrop.structure.api.Command;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class AggregateIdUtilsTest {

    @Test
    @DisplayName("AggregateIdUtils.getAggregateId() - Given an POJO can we deduce the AggregateId based on the annotation @AggregateId")
    void getAggregateId_byObject() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "Test");
        Optional<UUID> aggregateId = AggregateIdUtils.getAggregateId(command);
        assertThat(aggregateId.get(), is(command.getUserId()));
    }

    @Test
    @DisplayName("AggregateIdUtils.getAggregateId() - Given an AggregateRoot object can we deduce the AggregateId based on the annotation @AggregateId of the target")
    void getAggregateId_byAggregateRoot() {
        Method[] methodsWithAnnotation = MethodUtils.getMethodsWithAnnotation(DewdropUserAggregate.class, CommandHandler.class);
        Optional<AggregateRoot> optAggregate = AggregateUtils.createFromCommandHandlerMethod(methodsWithAnnotation[0]);
        AggregateRoot aggregateRoot = optAggregate.get();
        DewdropUserAggregate dewdropUserAggregate = (DewdropUserAggregate) aggregateRoot.getTarget();
        dewdropUserAggregate.setUserId(UUID.randomUUID());

        Optional<UUID> aggregateId = AggregateIdUtils.getAggregateId(aggregateRoot);
        assertThat(aggregateId.get(), is(dewdropUserAggregate.getUserId()));
    }

    @Test
    @DisplayName("AggregateIdUtils.getAggregateId() - Given an AggregateRoot object can we deduce the AggregateId based on the annotation @AggregateId of the target with no value")
    void getAggregateId_byAggregateRoot_noValue() {
        Method[] methodsWithAnnotation = MethodUtils.getMethodsWithAnnotation(DewdropUserAggregate.class, CommandHandler.class);
        Optional<AggregateRoot> optAggregate = AggregateUtils.createFromCommandHandlerMethod(methodsWithAnnotation[0]);
        AggregateRoot aggregateRoot = optAggregate.get();
        DewdropUserAggregate dewdropUserAggregate = (DewdropUserAggregate) aggregateRoot.getTarget();


        Optional<UUID> aggregateId = AggregateIdUtils.getAggregateId(aggregateRoot);
        assertThat(aggregateId.isEmpty(), is(true));
    }

    @Test
    @DisplayName("AggregateIdUtils.getAggregateId() - Given an AggregateRoot if we have a targetObject that does not contain a @AggregateId annotation")
    void getAggregateId_invalidTarget() {
        assertThrows(IllegalArgumentException.class, () -> AggregateIdUtils.getAggregateId(new NoAggregateIdCommand()));
        assertThrows(IllegalArgumentException.class, () -> AggregateIdUtils.getAggregateId("Test"));
        TooManyAggregateIds tooManyAggregateIds = new TooManyAggregateIds(UUID.randomUUID(), UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> AggregateIdUtils.getAggregateId(tooManyAggregateIds));
    }

    @Test
    @DisplayName("AggregateIdUtils.getAggregateId() - Given an AggregateRoot when we read the field value for the AggregateId we get an IllegalAccessException")
    void getAggregateId_exception() {
        try (MockedStatic<FieldUtils> utilities = mockStatic(FieldUtils.class)) {
            utilities.when(() -> FieldUtils.readField(any(Field.class), any(Object.class), anyBoolean())).thenThrow(new IllegalAccessException());

            DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "Test");
            assertThrows(RuntimeException.class, () -> AggregateIdUtils.getAggregateId(command));
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private class TooManyAggregateIds {
        @AggregateId
        UUID first;
        @AggregateId
        UUID second;
    }
    @Data
    @NoArgsConstructor
    private class NoAggregateIdCommand extends Command {
    }
}
