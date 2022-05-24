package com.dewdrop;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.dewdrop.api.result.Result;
import com.dewdrop.api.result.ResultException;
import com.dewdrop.config.DewdropProperties;
import com.dewdrop.config.DewdropSettings;
import com.dewdrop.fixture.CreateUserCommand;
import com.dewdrop.fixture.DewdropAccountDetails;
import com.dewdrop.fixture.DewdropAddFundsToAccountCommand;
import com.dewdrop.fixture.DewdropCreateAccountCommand;
import com.dewdrop.fixture.DewdropGetAccountByIdQuery;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
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

        retryUntilComplete(dewDrop, query, result);

        DewdropAccountDetails actual = result.get();
        assertThat(actual.getUsername(), is(username));
    }

    private void retryUntilComplete(Dewdrop dewDrop, DewdropGetAccountByIdQuery query, Result<DewdropAccountDetails> result) {
        BigDecimal balance = new BigDecimal(100);
        with().pollInterval(fibonacci(SECONDS))
            .await()
            .until(() -> {
                Result<DewdropAccountDetails> objectResult = dewDrop.executeQuery(query);
                if (objectResult.isValuePresent()) {
                    DewdropAccountDetails dewdropAccountDetails = objectResult.get();
                    if (StringUtils.isNotEmpty(dewdropAccountDetails
                        .getUsername()) && dewdropAccountDetails.getBalance()
                        .equals(balance)) {
                        result.of(objectResult);
                        return true;
                    }
                }
                return false;
            });
    }
    
}
