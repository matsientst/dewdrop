package com.dewdrop;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import com.dewdrop.api.result.Result;
import com.dewdrop.api.result.ResultException;
import com.dewdrop.config.DewdropProperties;
import com.dewdrop.config.DewdropSettings;
import com.dewdrop.fixture.command.DewdropAddFundsToAccountCommand;
import com.dewdrop.fixture.command.DewdropCreateAccountCommand;
import com.dewdrop.fixture.command.DewdropCreateUserCommand;
import com.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetails;
import com.dewdrop.fixture.readmodel.accountdetails.details.DewdropGetAccountByIdQuery;
import com.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummary;
import com.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummaryQuery;
import com.dewdrop.fixture.readmodel.users.DewdropUser;
import com.dewdrop.fixture.readmodel.users.GetUserById;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Log4j2
class DewdropTest {
    Executor delayed = CompletableFuture.delayedExecutor(10L, SECONDS);
    DewdropProperties properties = DewdropProperties.builder()
        .packageToScan("com.dewdrop")
        .packageToExclude("com.dewdrop.fixture.customized")
        .connectionString("esdb://localhost:2113?tls=false")
        .create();

    @Test
    @Disabled
    void test() throws ResultException {
        Dewdrop dewDrop = DewdropSettings.builder()
            .properties(properties)
            .create()
            .start();

        String username = "Dewdropper Funkapuss";
        DewdropCreateUserCommand createUserCommand = new DewdropCreateUserCommand(UUID.randomUUID(), username);
        dewDrop.executeCommand(createUserCommand);

        DewdropCreateAccountCommand command = new DewdropCreateAccountCommand(UUID.randomUUID(), "test", createUserCommand.getUserId());
        dewDrop.executeCommand(command);

        DewdropAddFundsToAccountCommand addFunds = new DewdropAddFundsToAccountCommand(command.getAccountId(), new BigDecimal(100));
        dewDrop.executeSubsequentCommand(addFunds, command);

        DewdropGetAccountByIdQuery query = new DewdropGetAccountByIdQuery(command.getAccountId());
        retryUntilComplete(dewDrop, query);

        Result<DewdropAccountDetails> result = dewDrop.executeQuery(query);
        DewdropAccountDetails actual = result.get();
        assertThat(actual.getUsername(), is(username));
        assertThat(actual.getBalance(), is(addFunds.getFunds()));

        GetUserById getUserById = new GetUserById(createUserCommand.getUserId());
        Result<DewdropUser> userById = dewDrop.executeQuery(getUserById);
        DewdropUser dewdropUser = userById.get();
        assertThat(createUserCommand.getUserId(), is(dewdropUser
            .getUserId()));
        assertThat(createUserCommand.getUsername(), is(dewdropUser.getUsername()));

        DewdropAccountSummaryQuery dewdropAccountSummaryQuery = new DewdropAccountSummaryQuery();
        Result<DewdropAccountSummary> summaryResult = dewDrop.executeQuery(dewdropAccountSummaryQuery);
        BigDecimal totalFunds = summaryResult.get()
            .getTotalFunds();
        assertThat(totalFunds, is(greaterThan(new BigDecimal(99))));
        int countOfAccounts = summaryResult.get()
            .getCountOfAccounts();
        assertThat(countOfAccounts, is(greaterThan(0)));

        log.info("TOTAL ACCOUNTS: {}", countOfAccounts);
        assertThat(totalFunds, is(new BigDecimal(100).multiply(new BigDecimal(countOfAccounts))));
    }

    private void retryUntilComplete(Dewdrop dewdrop, DewdropGetAccountByIdQuery query) {
        BigDecimal balance = new BigDecimal(100);

        with().pollInterval(fibonacci(SECONDS))
            .await()
            .until(() -> {
                Result<DewdropAccountDetails> objectResult = dewdrop.executeQuery(query);
                if (objectResult.isValuePresent()) {
                    DewdropAccountDetails dewdropAccountDetails = objectResult.get();
                    if (StringUtils.isNotEmpty(dewdropAccountDetails.getUsername()) && dewdropAccountDetails.getBalance()
                        .equals(balance)) {
                        return true;
                    }
                }
                return false;
            });
    }

}
