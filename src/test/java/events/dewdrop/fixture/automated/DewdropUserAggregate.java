package events.dewdrop.fixture.automated;

import events.dewdrop.aggregate.annotation.Aggregate;
import events.dewdrop.aggregate.annotation.AggregateId;
import events.dewdrop.api.validators.ValidationException;
import events.dewdrop.command.CommandHandler;
import events.dewdrop.fixture.command.DewdropCreateUserCommand;
import events.dewdrop.fixture.events.DewdropUserCreated;
import events.dewdrop.read.readmodel.annotation.EventHandler;
import events.dewdrop.structure.api.validator.DewdropValidator;
import java.util.UUID;
import lombok.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Aggregate
@Data
public class DewdropUserAggregate {
    @AggregateId
    UUID userId;
    private String username;

    public DewdropUserAggregate() {}

    @CommandHandler
    public DewdropUserCreated createUser(DewdropCreateUserCommand command) throws ValidationException {
        DewdropValidator.validate(command);
        return new DewdropUserCreated(command.getUserId(), command.getUsername());
    }

    @EventHandler
    public void on(DewdropUserCreated userCreated) {
        this.userId = userCreated.getUserId();
        this.username = userCreated.getUsername();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }

        if (o == null || getClass() != o.getClass()) { return false; }

        DewdropUserAggregate that = (DewdropUserAggregate) o;

        return new EqualsBuilder().append(userId, that.userId).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(userId).toHashCode();
    }
}
