package events.dewdrop;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import events.dewdrop.api.result.Result;
import events.dewdrop.api.validators.ValidationException;
import events.dewdrop.config.DewdropProperties;
import events.dewdrop.config.DewdropSettings;
import events.dewdrop.fixture.command.DewdropAddFundsToAccountCommand;
import events.dewdrop.fixture.command.DewdropCreateAccountCommand;
import events.dewdrop.fixture.command.DewdropCreateUserCommand;
import events.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetails;
import events.dewdrop.fixture.readmodel.accountdetails.details.DewdropGetAccountByIdQuery;
import events.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummary;
import events.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummaryQuery;
import events.dewdrop.fixture.readmodel.users.DewdropUser;
import events.dewdrop.fixture.readmodel.users.GetUserByIdQuery;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

@Log4j2
class DewdropTest {
    DewdropProperties properties = DewdropProperties.builder().packageToScan("events.dewdrop").packageToExclude("events.dewdrop.fixture.customized").connectionString("esdb://localhost:2113?tls=false").create();

    @Test
    void test() throws ValidationException {
        Dewdrop dewdrop = DewdropSettings.builder().properties(properties).create().start();

        DewdropCreateUserCommand createUserCommand = createUser(dewdrop);
        DewdropCreateAccountCommand createAccountCommand = createAccount(dewdrop, createUserCommand);
        DewdropAddFundsToAccountCommand addFunds = addFunds(dewdrop, createAccountCommand);

        DewdropGetAccountByIdQuery query = new DewdropGetAccountByIdQuery(createAccountCommand.getAccountId());
        BigDecimal balance = new BigDecimal(100);
        retryUntilComplete(dewdrop, query, (result) -> {
            DewdropAccountDetails dewdropAccountDetails = (DewdropAccountDetails) result.get();
            if (StringUtils.isNotEmpty(dewdropAccountDetails.getUsername()) && dewdropAccountDetails.getBalance().equals(balance)) { return true; }
            return false;
        });

        Result<DewdropAccountDetails> result = dewdrop.executeQuery(query);
        DewdropAccountDetails actual = result.get();
        assertThat(actual.getUsername(), is(createUserCommand.getUsername()));
        assertThat(actual.getBalance(), is(addFunds.getFunds()));

        DewdropCreateUserCommand userCommand = createUser(dewdrop);
        createAccountCommand = createAccount(dewdrop, createUserCommand);
        addFunds = addFunds(dewdrop, createAccountCommand);

        GetUserByIdQuery getUserById = new GetUserByIdQuery(createUserCommand.getUserId());
        retryUntilComplete(dewdrop, getUserById, (userResult) -> {
            log.info("userResult: {}", userResult);
            if (!userResult.isValuePresent()) { return false; }
            DewdropUser dewdropUser = (DewdropUser) userResult.get();
            if (StringUtils.isNotEmpty(dewdropUser.getUsername()) && dewdropUser.getUserId().equals(createUserCommand.getUserId())) { return true; }
            return false;
        });
        Result<DewdropUser> userResult = dewdrop.executeQuery(getUserById);
        DewdropUser dewdropUser = userResult.get();
        assertThat(createUserCommand.getUserId(), is(dewdropUser.getUserId()));
        assertThat(createUserCommand.getUsername(), is(dewdropUser.getUsername()));

        DewdropAccountSummaryQuery dewdropAccountSummaryQuery = new DewdropAccountSummaryQuery();
        retryUntilComplete(dewdrop, dewdropAccountSummaryQuery, (summaryResult) -> {
            if (!summaryResult.isValuePresent()) { return false; }
            DewdropAccountSummary summary = (DewdropAccountSummary) summaryResult.get();
            if (summary.getTotalFunds().equals(new BigDecimal(100).multiply(new BigDecimal(summary.getCountOfAccounts())))) {
                log.info("TOTAL ACCOUNTS: {}", summary.getCountOfAccounts());
                return true;
            }
            return false;
        });
    }

    private DewdropAddFundsToAccountCommand addFunds(Dewdrop dewdrop, DewdropCreateAccountCommand command) throws ValidationException {
        DewdropAddFundsToAccountCommand addFunds = new DewdropAddFundsToAccountCommand(command.getAccountId(), new BigDecimal(100));
        dewdrop.executeSubsequentCommand(addFunds, command);
        return addFunds;
    }

    private DewdropCreateAccountCommand createAccount(Dewdrop dewdrop, DewdropCreateUserCommand createUserCommand) throws ValidationException {
        DewdropCreateAccountCommand command = new DewdropCreateAccountCommand(UUID.randomUUID(), "test", createUserCommand.getUserId());
        dewdrop.executeCommand(command);
        return command;
    }

    private DewdropCreateUserCommand createUser(Dewdrop dewdrop) throws ValidationException {
        DewdropCreateUserCommand createUserCommand = new DewdropCreateUserCommand(UUID.randomUUID(), "Dewdropper Funkapuss");
        dewdrop.executeCommand(createUserCommand);
        return createUserCommand;
    }

    private <T> void retryUntilComplete(Dewdrop dewdrop, Object query, Predicate<Result> predicate) {
        with().pollInterval(fibonacci(SECONDS)).await().timeout(100000L, SECONDS).until(() -> predicate.test(dewdrop.executeQuery(query)));
    }

}
