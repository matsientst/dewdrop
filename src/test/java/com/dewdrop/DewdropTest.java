package com.dewdrop;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.dewdrop.api.result.Result;
import com.dewdrop.api.result.ResultException;
import com.dewdrop.command.CommandHandlerMapper;
import com.dewdrop.config.DewdropProperties;
import com.dewdrop.config.DewdropSettings;
import com.dewdrop.fixture.DewdropAccountDetails;
import com.dewdrop.fixture.DewdropAddFundsToAccountCommand;
import com.dewdrop.fixture.DewdropCreateAccountCommand;
import com.dewdrop.fixture.DewdropGetAccountByIdQuery;
import com.dewdrop.fixture.DewdropStandaloneCommandService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

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

        DewdropCreateAccountCommand command = new DewdropCreateAccountCommand(UUID.randomUUID(), "test");
        dewDrop.onCommand(command);

        DewdropAddFundsToAccountCommand addFunds = new DewdropAddFundsToAccountCommand(command.getAccountId(), new BigDecimal(100));
        dewDrop.onCommand(addFunds);

        DewdropGetAccountByIdQuery query = new DewdropGetAccountByIdQuery(command.getAccountId());
        Result<DewdropAccountDetails> result = dewDrop.onQuery(query);
        assertThat(result.get(), is(notNullValue()));
    }

    @Test
    void test_commandHandler() {
        Dewdrop dewDrop = DewdropSettings.builder()
            .properties(properties)
            .commandMapper(new CommandHandlerMapper())
            .create()
            .start();

        DewdropCreateAccountCommand command = new DewdropCreateAccountCommand(UUID.randomUUID(), "test");
        dewDrop.onCommand(command);

        DewdropAddFundsToAccountCommand addFunds = new DewdropAddFundsToAccountCommand(command.getAccountId(), new BigDecimal(100));
        dewDrop.onCommand(addFunds);
    }

    @Test
    void test_standalone_subclassOfAggregateRoot() {
        Dewdrop dewDrop = DewdropSettings.builder()
            .properties(properties)
            .create()
            .start();

        DewdropStandaloneCommandService commandService = new DewdropStandaloneCommandService(dewDrop.getSettings()
            .getStreamStoreRepository());

        DewdropCreateAccountCommand command = new DewdropCreateAccountCommand(UUID.randomUUID(), "test");
        commandService.process(command);

        DewdropAddFundsToAccountCommand addFunds = new DewdropAddFundsToAccountCommand(command.getAccountId(), new BigDecimal(100));
        commandService.process(addFunds);
    }
}
