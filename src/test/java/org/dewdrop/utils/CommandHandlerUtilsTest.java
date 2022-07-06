package org.dewdrop.utils;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import org.dewdrop.aggregate.AggregateRoot;
import org.dewdrop.api.result.Result;
import org.dewdrop.api.result.ResultException;
import org.dewdrop.command.CommandHandler;
import org.dewdrop.fixture.automated.DewdropUserAggregate;
import org.dewdrop.fixture.command.DewdropAddFundsToAccountCommand;
import org.dewdrop.fixture.command.DewdropCreateUserCommand;
import org.dewdrop.fixture.customized.DewdropCommandService;
import org.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import org.dewdrop.fixture.events.DewdropUserCreated;
import org.dewdrop.structure.api.Command;
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
        ReflectionsConfigUtils.init("org.dewdrop");
        commandMethods = CommandHandlerUtils.getCommandHandlerMethods().stream().filter(item -> item.getDeclaringClass().getSimpleName().equals(DewdropUserAggregate.class.getSimpleName()))
                        .collect(toMap((key) -> key.getParameterTypes()[0], Function.identity()));
    }

    @Test
    @DisplayName("executeCommand() - Given a target, a method, a command and an aggregate root, When the method is called, Then the method is called with the command and the aggregate root")
    void executeCommand() throws ResultException {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "test");

        Method method = commandMethods.get(DewdropCreateUserCommand.class);
        Optional<AggregateRoot> aggregateRoot = AggregateUtils.createFromCommandHandlerMethod(method);
        Result<DewdropUserCreated> events = CommandHandlerUtils.executeCommand(target, method, command, aggregateRoot.get());
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

            Result<DewdropUserCreated> events = CommandHandlerUtils.executeCommand(method, command, aggregateRoot.get());
            assertThat(events.isValuePresent(), is(false));
        }
    }

    @Test
    @DisplayName("executeCommand() - Given a command handler method with a second parameter, when we call executeCommand(), then the second parameter is passed to the method")
    void executeCommand_multipleParams() throws ResultException {
        ReflectionsConfigUtils.init("org.dewdrop", List.of("org.dewdrop.fixtures.automated"));
        DewdropAddFundsToAccountCommand command = new DewdropAddFundsToAccountCommand(UUID.randomUUID(), new BigDecimal(100));

        Method method = MethodUtils.getMethodsWithAnnotation(DewdropCommandService.class, CommandHandler.class)[0];
        Optional<AggregateRoot> aggregateRoot = AggregateUtils.createFromCommandHandlerMethod(method);
        Result<List<DewdropFundsAddedToAccount>> events = CommandHandlerUtils.executeCommand(new DewdropCommandService(), method, command, aggregateRoot.get());
        assertThat(events.get().get(0).getFunds(), is(command.getFunds()));
    }
}
