package events.dewdrop.fixture.automated;

import events.dewdrop.aggregate.annotation.Aggregate;
import events.dewdrop.aggregate.annotation.AggregateId;
import events.dewdrop.api.validators.ValidationException;
import events.dewdrop.command.CommandHandler;
import events.dewdrop.read.readmodel.annotation.EventHandler;
import events.dewdrop.fixture.command.DewdropAddFundsToAccountCommand;
import events.dewdrop.fixture.command.DewdropCreateAccountCommand;
import events.dewdrop.fixture.events.DewdropAccountCreated;
import events.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import events.dewdrop.structure.api.validator.DewdropValidator;

@Data
@Aggregate
public class DewdropAccountAggregate {
    @AggregateId
    UUID accountId;
    String name;
    BigDecimal balance = BigDecimal.ZERO;

    public DewdropAccountAggregate() {}

    @CommandHandler
    public List<DewdropAccountCreated> handle(DewdropCreateAccountCommand command) throws ValidationException {
        DewdropValidator.validate(command);

        return List.of(new DewdropAccountCreated(command.getAccountId(), command.getName(), command.getUserId()));
    }

    @CommandHandler
    public List<DewdropFundsAddedToAccount> handle(DewdropAddFundsToAccountCommand command) {
        if (command.getAccountId() == null) { throw new IllegalArgumentException("Id cannot be empty"); }

        DewdropFundsAddedToAccount dewdropFundsAddedToAccount = new DewdropFundsAddedToAccount(command.getAccountId(), command.getFunds());
        return List.of(dewdropFundsAddedToAccount);
    }

    @EventHandler
    public void on(DewdropAccountCreated event) {
        // validate here as well different
        // check that teh aggregate invariance are always true
        this.accountId = event.getAccountId();
        this.name = event.getName();
        // DewdropAccountAggregate.from(this).with();
    }

    @EventHandler
    public void on(DewdropFundsAddedToAccount event) {
        // this.accountId = event.getAccountId();
        this.balance = this.balance.add(event.getFunds());
    }
}
