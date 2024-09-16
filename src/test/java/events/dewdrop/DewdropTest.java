package events.dewdrop;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import events.dewdrop.api.result.Result;
import events.dewdrop.api.validators.ValidationException;
import events.dewdrop.config.DewdropProperties;
import events.dewdrop.config.DewdropSettings;
import events.dewdrop.fixture.command.DewdropAddFundsToAccountCommand;
import events.dewdrop.fixture.command.DewdropCreateAccountCommand;
import events.dewdrop.fixture.command.DewdropCreateUserCommand;
import events.dewdrop.fixture.command.DewdropDeactivateUserCommand;
import events.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetails;
import events.dewdrop.fixture.readmodel.accountdetails.details.DewdropGetAccountByIdQuery;
import events.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummary;
import events.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummaryQuery;
import events.dewdrop.fixture.readmodel.users.DewdropGetUserByIdQuery;
import events.dewdrop.fixture.readmodel.users.DewdropGetUserByIdQueryForAggregate;
import events.dewdrop.fixture.readmodel.users.DewdropUser;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

@Log4j2
class DewdropTest {
    DewdropProperties properties = DewdropProperties.builder().packageToScan("events.dewdrop").packageToExclude(List.of("events.dewdrop.fixture.customized", "events.dewdrop.fixture.readmodel.users.lifecycle"))
                    .connectionString("esdb://localhost:2113?tls=false").create();

    @Test
    void test() throws ValidationException, ExecutionException, InterruptedException {

        Dewdrop dewdrop = CompletableFuture.supplyAsync(() -> DewdropSettings.builder().properties(properties).create().start()).get();

        DewdropCreateUserCommand createUserCommand = createUser(dewdrop);
        DewdropCreateAccountCommand createAccountCommand = createAccount(dewdrop, createUserCommand);
        final UUID causationId = createAccountCommand.getCausationId();
        final DewdropAddFundsToAccountCommand addFunds = addFunds(dewdrop, createAccountCommand);

        DewdropGetAccountByIdQuery query = new DewdropGetAccountByIdQuery(createAccountCommand.getAccountId());
        BigDecimal balance = new BigDecimal(100);
        retryUntilComplete(dewdrop, query, (result) -> {
            if (result.isEmpty()) {
                log.info("Query not found:{}", query);
                return false;
            }
            DewdropAccountDetails dewdropAccountDetails = (DewdropAccountDetails) result.get();

            if (StringUtils.isNotEmpty(dewdropAccountDetails.getUsername()) && dewdropAccountDetails.getBalance().equals(balance)) {
                assertEquals(causationId, dewdropAccountDetails.getCausationId());
                return true;
            }
            return false;
        });
        retryUntilComplete(dewdrop, query, (result) -> {
            if (result.isEmpty()) {
                log.info("Query not found:{}", query);
                return false;
            }
            DewdropAccountDetails actual = (DewdropAccountDetails) result.get();
            assertThat(actual.getUsername(), is(createUserCommand.getUsername()));
            assertThat(actual.getBalance(), is(addFunds.getFunds()));
            return true;
        });

        createUser(dewdrop);
        createAccountCommand = createAccount(dewdrop, createUserCommand);
        addFunds(dewdrop, createAccountCommand);

        DewdropGetUserByIdQuery getUserById = new DewdropGetUserByIdQuery(createUserCommand.getUserId());
        retryUntilComplete(dewdrop, getUserById, (userResult) -> {
            if (userResult.isEmpty()) {
                log.info("Query not found:{}", query);
                return false;
            }
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
            if (summaryResult.isEmpty()) {
                log.info("Query not found:{}", query);
                return false;
            }
            DewdropAccountSummary summary = (DewdropAccountSummary) summaryResult.get();
            if (summary.getTotalFunds().equals(new BigDecimal(100).multiply(new BigDecimal(summary.getCountOfAccounts())))) {
                log.info("TOTAL ACCOUNTS: {}", summary.getCountOfAccounts());
                return true;
            }
            return false;
        });

        DewdropDeactivateUserCommand deactivateCommand = new DewdropDeactivateUserCommand(createUserCommand.getUserId());
        dewdrop.executeCommand(deactivateCommand);

        DewdropGetUserByIdQueryForAggregate aggregateUserById = new DewdropGetUserByIdQueryForAggregate(createUserCommand.getUserId());

        retryUntilComplete(dewdrop, aggregateUserById, (deactivatedUser) -> {
            if (deactivatedUser.isEmpty()) {
                log.info("Query not found:{}", query);
                return false;
            }
            DewdropUser innactiveUser = (DewdropUser) deactivatedUser.get();
            assertThat(innactiveUser.isActive(), is(false));
            return true;
        });
    }

    private DewdropAddFundsToAccountCommand addFunds(Dewdrop dewdrop, DewdropCreateAccountCommand command) throws ValidationException {
        DewdropAddFundsToAccountCommand addFunds = new DewdropAddFundsToAccountCommand(command.getAccountId(), new BigDecimal(100));
        dewdrop.executeSubsequentCommand(addFunds, command);
        return addFunds;
    }

    private DewdropCreateAccountCommand createAccount(Dewdrop dewdrop, DewdropCreateUserCommand createUserCommand) throws ValidationException {
        DewdropCreateAccountCommand command = new DewdropCreateAccountCommand(UUID.randomUUID(), "test", createUserCommand.getUserId());
        command.setCausationId(UUID.randomUUID());
        dewdrop.executeCommand(command);
        return command;
    }

    private DewdropCreateUserCommand createUser(Dewdrop dewdrop) throws ValidationException, ExecutionException, InterruptedException {
        CompletableFuture<DewdropCreateUserCommand> command = CompletableFuture.supplyAsync(() -> {
            try {
                DewdropCreateUserCommand createUserCommand = new DewdropCreateUserCommand(UUID.randomUUID(), "Dewdropper Funkapuss");
                dewdrop.executeCommand(createUserCommand);
                return createUserCommand;
            } catch (ValidationException e) {
                fail();
                return null;
            }
        });
        return command.get();
    }

    private <T> void retryUntilComplete(Dewdrop dewdrop, Object query, Predicate<Result> predicate) {
        with().pollInterval(fibonacci(SECONDS)).await().timeout(100000L, SECONDS).until(() -> predicate.test(dewdrop.executeQuery(query)));
    }

}
