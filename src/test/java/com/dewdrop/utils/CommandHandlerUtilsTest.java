package com.dewdrop.utils;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.command.CommandHandler;
import com.dewdrop.fixture.automated.DewdropUserAggregate;
import com.dewdrop.fixture.command.DewdropAddFundsToAccountCommand;
import com.dewdrop.fixture.command.DewdropCreateUserCommand;
import com.dewdrop.fixture.customized.DewdropCommandService;
import com.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import com.dewdrop.fixture.events.DewdropUserCreated;
import com.dewdrop.structure.api.Command;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class CommandHandlerUtilsTest {
    DewdropUserAggregate target = new DewdropUserAggregate();
    Map<Class<?>, Method> commandMethods;

    @BeforeEach
    void setup() {
        ReflectionsConfigUtils.init("com.dewdrop");
        commandMethods = CommandHandlerUtils.getCommandHandlerMethods().stream().filter(item -> item.getDeclaringClass().getSimpleName().equals(DewdropUserAggregate.class.getSimpleName()))
                        .collect(toMap((key) -> key.getParameterTypes()[0], Function.identity()));
    }

    @Test
    @DisplayName("executeCommand() - Given a target, a method, a command and an aggregate root, When the method is called, Then the method is called with the command and the aggregate root")
    void executeCommand() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "test");

        Method method = commandMethods.get(DewdropCreateUserCommand.class);
        Optional<AggregateRoot> aggregateRoot = AggregateUtils.createFromCommandHandlerMethod(method);
        Optional<DewdropUserCreated> events = CommandHandlerUtils.executeCommand(target, method, command, aggregateRoot.get());
        assertThat(events.get().getUserId(), is(command.getUserId()));
    }

    @Test
    @DisplayName("executeCommand() - Given a command handler method, a command and an aggregate root, when an exception is thrown, then return an Optional.empty()")
    void executeCommand_exception() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "test");

        Method method = commandMethods.get(DewdropCreateUserCommand.class);
        Optional<AggregateRoot> aggregateRoot = AggregateUtils.createFromCommandHandlerMethod(method);
        try (MockedStatic<DewdropReflectionUtils> utilities = mockStatic(DewdropReflectionUtils.class)) {
            utilities.when(() -> DewdropReflectionUtils.callMethod(any(), anyString(), any(Command.class))).thenThrow(new IllegalArgumentException());

            Optional<DewdropUserCreated> events = CommandHandlerUtils.executeCommand(method, command, aggregateRoot.get());
            assertThat(events.isPresent(), is(false));
        }
    }

    @Test
    @DisplayName("executeCommand() - Given a command handler method with a second parameter, when we call executeCommand(), then the second parameter is passed to the method")
    void executeCommand_multipleParams() {
        ReflectionsConfigUtils.init("com.dewdrop", List.of("com.dewdrop.fixtures.automated"));
        DewdropAddFundsToAccountCommand command = new DewdropAddFundsToAccountCommand(UUID.randomUUID(), new BigDecimal(100));

        Method method = MethodUtils.getMethodsWithAnnotation(DewdropCommandService.class, CommandHandler.class)[0];
        Optional<AggregateRoot> aggregateRoot = AggregateUtils.createFromCommandHandlerMethod(method);
        Optional<List<DewdropFundsAddedToAccount>> events = CommandHandlerUtils.executeCommand(new DewdropCommandService(), method, command, aggregateRoot.get());
        assertThat(events.get().get(0).getFunds(), is(command.getFunds()));
    }
}
