package events.dewdrop;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;

import events.dewdrop.api.result.Result;
import events.dewdrop.api.validators.ValidationException;
import events.dewdrop.config.DewdropProperties;
import events.dewdrop.config.DewdropSettings;
import events.dewdrop.fixture.command.user.UserClaimUsernameCommand;
import events.dewdrop.fixture.command.user.UserSignupCommand;
import events.dewdrop.fixture.readmodel.users.GetUserByIdQuery;
import events.dewdrop.fixture.readmodel.users.User;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

@Log4j2
public class UserLifecycleTest {

    DewdropProperties properties = DewdropProperties.builder().packageToScan("events.dewdrop").packageToExclude("events.dewdrop.fixture.customized").connectionString("esdb://localhost:2113?tls=false").create();

    @Test
    void test() throws ValidationException {
        Dewdrop dewdrop = DewdropSettings.builder().properties(properties).create().start();

        UserSignupCommand userSignupCommand = createUser(dewdrop);
        UserClaimUsernameCommand userClaimUsernameCommand = claimUsername(userSignupCommand.getUserId(), dewdrop);

        GetUserByIdQuery getUserById = new GetUserByIdQuery(userClaimUsernameCommand.getUserId());
        retryUntilComplete(dewdrop, getUserById, (userResult) -> {
            log.info("userResult: {}", userResult);
            if (!userResult.isValuePresent()) { return false; }
            User user = (User) userResult.get();
            if (StringUtils.isNotEmpty(user.getUsername()) && user.getUserId().equals(userSignupCommand.getUserId())) { return true; }
            return false;
        });
    }

    private UserSignupCommand createUser(Dewdrop dewdrop) throws ValidationException {
        UserSignupCommand userSignupCommand = new UserSignupCommand(UUID.randomUUID(), "funkapuss@dendritemalfunction.com");
        dewdrop.executeCommand(userSignupCommand);
        return userSignupCommand;
    }

    private UserClaimUsernameCommand claimUsername(UUID userId, Dewdrop dewdrop) throws ValidationException {
        UserClaimUsernameCommand claimUsernameCommand = new UserClaimUsernameCommand(userId, "funkapuss sassafrass");
        dewdrop.executeCommand(claimUsernameCommand);
        return claimUsernameCommand;
    }

    private <T> void retryUntilComplete(Dewdrop dewdrop, Object query, Predicate<Result> predicate) {
        with().pollInterval(fibonacci(SECONDS)).await().timeout(100000L, SECONDS).until(() -> predicate.test(dewdrop.executeQuery(query)));
    }
}
