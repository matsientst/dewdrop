package events.dewdrop;

import events.dewdrop.api.validators.ValidationException;
import events.dewdrop.config.DewdropProperties;
import events.dewdrop.config.DewdropSettings;
import events.dewdrop.fixture.command.user.UserClaimUsernameCommand;
import events.dewdrop.fixture.command.user.UserSignupCommand;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;

@Log4j2
public class UserSignupTest {

    DewdropProperties properties = DewdropProperties.builder().packageToScan("events.dewdrop").packageToExclude("events.dewdrop.fixture.customized").connectionString("esdb://localhost:2113?tls=false").create();

    @Test
    void test() throws ValidationException {
        Dewdrop dewdrop = DewdropSettings.builder().properties(properties).create().start();

        UserSignupCommand userSignupCommand = createUser(dewdrop);
        UserClaimUsernameCommand userClaimUsernameCommand = claimUsername(userSignupCommand.getUserId(), dewdrop);


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


}
