package com.dewdrop;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.dewdrop.api.result.Result;
import com.dewdrop.api.result.ResultException;
import com.dewdrop.command.CommandHandlerMapper;
import com.dewdrop.config.DewdropProperties;
import com.dewdrop.config.DewdropSettings;
import com.dewdrop.fixture.CreateUserCommand;
import com.dewdrop.fixture.DewdropAccountDetails;
import com.dewdrop.fixture.DewdropAddFundsToAccountCommand;
import com.dewdrop.fixture.DewdropCreateAccountCommand;
import com.dewdrop.fixture.DewdropGetAccountByIdQuery;
import com.dewdrop.fixture.DewdropStandaloneCommandService;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
@Log4j2
class DewdropTest {
    DewdropProperties properties = DewdropProperties.builder()
        .packageToScan("com.dewdrop")
        .connectionString("esdb://localhost:2113?tls=false")
        .create();

    @Test
    void test() throws ResultException {
        Dewdrop dewDrop = DewdropSettings.builder()
            .properties(properties)
            .create()
            .start();

        String username = "Dewdropper Funkapuss";
        CreateUserCommand createUserCommand = new CreateUserCommand(UUID.randomUUID(), username);
        dewDrop.executeCommand(createUserCommand);

        DewdropCreateAccountCommand command = new DewdropCreateAccountCommand(UUID.randomUUID(), "test", createUserCommand.getUserId());
        dewDrop.executeCommand(command);

        DewdropAddFundsToAccountCommand addFunds = new DewdropAddFundsToAccountCommand(command.getAccountId(), new BigDecimal(100));
        dewDrop.executeSubsequentCommand(addFunds, command);

        DewdropGetAccountByIdQuery query = new DewdropGetAccountByIdQuery(command.getAccountId());
        Result<DewdropAccountDetails> result = dewDrop.executeQuery(query);
        result = dewDrop.executeQuery(query);
        DewdropAccountDetails actual = result.get();
        assertThat(actual.getUsername(), is(username));
    }

//    @Test
//    void test_commandHandler() {
//        Dewdrop dewDrop = DewdropSettings.builder()
//            .properties(properties)
//            .commandMapper(new CommandHandlerMapper())
//            .create()
//            .start();
//
//        DewdropCreateAccountCommand command = new DewdropCreateAccountCommand(UUID.randomUUID(), "test");
//        dewDrop.executeCommand(command);
//
//        DewdropAddFundsToAccountCommand addFunds = new DewdropAddFundsToAccountCommand(command.getAccountId(), new BigDecimal(100));
//        dewDrop.executeCommand(addFunds);
//    }
//
//    @Test
//    void test_standalone_subclassOfAggregateRoot() {
//        Dewdrop dewDrop = DewdropSettings.builder()
//            .properties(properties)
//            .create()
//            .start();
//
//        DewdropStandaloneCommandService commandService = new DewdropStandaloneCommandService(dewDrop.getSettings()
//            .getStreamStoreRepository());
//
//        DewdropCreateAccountCommand command = new DewdropCreateAccountCommand(UUID.randomUUID(), "test");
//        commandService.process(command);
//
//        DewdropAddFundsToAccountCommand addFunds = new DewdropAddFundsToAccountCommand(command.getAccountId(), new BigDecimal(100));
//        commandService.process(addFunds);
//    }
}
