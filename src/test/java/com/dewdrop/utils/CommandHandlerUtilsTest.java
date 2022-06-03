package com.dewdrop.utils;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.command.CommandHandler;
import com.dewdrop.fixture.automated.DewdropUserAggregate;
import com.dewdrop.fixture.command.DewdropAddFundsToAccountCommand;
import com.dewdrop.fixture.command.DewdropCreateUserCommand;
import com.dewdrop.fixture.customized.DewdropCommandService;
import com.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import com.dewdrop.fixture.events.DewdropUserCreated;
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
    @DisplayName("assignTo() - Given an object does it have the field associated with it")
    void executeCommand() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "test");

        Method method = commandMethods.get(DewdropCreateUserCommand.class);
        Optional<AggregateRoot> aggregateRoot = AggregateUtils.createFromCommandHandlerMethod(method);
        Optional<DewdropUserCreated> events = (Optional<DewdropUserCreated>) CommandHandlerUtils.executeCommand(target, method, command, aggregateRoot.get());
        assertThat(events.get().getUserId(), is(command.getUserId()));
    }

    @Test
    @DisplayName("assignTo() - Given an object does it have the field associated with it")
    void executeCommand_multipleParams() {
        ReflectionsConfigUtils.init("com.dewdrop", List.of("com.dewdrop.fixtures.automated"));
        DewdropAddFundsToAccountCommand command = new DewdropAddFundsToAccountCommand(UUID.randomUUID(), new BigDecimal(100));

        Method method = MethodUtils.getMethodsWithAnnotation(DewdropCommandService.class, CommandHandler.class)[0];
        Optional<AggregateRoot> aggregateRoot = AggregateUtils.createFromCommandHandlerMethod(method);
        Optional<List<DewdropFundsAddedToAccount>> events = (Optional<List<DewdropFundsAddedToAccount>>) CommandHandlerUtils.executeCommand(new DewdropCommandService(), method, command, aggregateRoot.get());
        assertThat(events.get().get(0).getFunds(), is(command.getFunds()));
    }
}
