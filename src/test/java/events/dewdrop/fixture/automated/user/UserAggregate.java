package events.dewdrop.fixture.automated.user;

import events.dewdrop.aggregate.annotation.Aggregate;
import events.dewdrop.aggregate.annotation.AggregateId;
import events.dewdrop.api.validators.ValidationException;
import events.dewdrop.command.CommandHandler;
import events.dewdrop.fixture.command.user.CsrClaimUsernameCommand;
import events.dewdrop.fixture.command.user.UserClaimUsernameCommand;
import events.dewdrop.fixture.command.user.UserSignupCommand;
import events.dewdrop.fixture.events.user.CsrClaimedUsername;
import events.dewdrop.fixture.events.user.UserClaimedUsername;
import events.dewdrop.fixture.events.user.UserSignedUp;
import events.dewdrop.read.readmodel.annotation.EventHandler;
import events.dewdrop.structure.api.validator.DewdropValidator;
import java.util.UUID;
import lombok.Data;

@Aggregate
@Data
public class UserAggregate {
    @AggregateId
    UUID userId;
    private String username;
    private String email;

    public UserAggregate() {}

    @CommandHandler
    public UserSignedUp signup(UserSignupCommand command) throws ValidationException {
        DewdropValidator.validate(command);
        return new UserSignedUp(command.getUserId(), command.getEmail());
    }

    @CommandHandler
    public UserClaimedUsername userClaimedUsername(UserClaimUsernameCommand command) throws ValidationException {
        DewdropValidator.validate(command);
        return new UserClaimedUsername(command.getUserId(), command.getUsername());
    }

    @CommandHandler
    public CsrClaimedUsername csrClaimedUsername(CsrClaimUsernameCommand command) throws ValidationException {
        DewdropValidator.validate(command);
        return new CsrClaimedUsername(command.getUserId(), command.getUsername());
    }

    // reportoffensive


    @EventHandler
    public void on(UserSignedUp userSignedup) {
        this.userId = userSignedup.getUserId();
        this.email = userSignedup.getEmail();
    }

    @EventHandler
    public void on(UserClaimedUsername userClaimedUsername) {
        this.userId = userClaimedUsername.getUserId();
        this.username = userClaimedUsername.getUsername();
    }

    @EventHandler
    public void on(CsrClaimedUsername csrClaimedUsername) {
        this.userId = csrClaimedUsername.getUserId();
        this.username = csrClaimedUsername.getUsername();
    }

    // reported offensive
}
