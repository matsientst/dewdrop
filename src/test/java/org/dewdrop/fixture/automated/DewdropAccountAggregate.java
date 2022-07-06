package org.dewdrop.fixture.automated;

import org.dewdrop.aggregate.annotation.Aggregate;
import org.dewdrop.aggregate.annotation.AggregateId;
import org.dewdrop.command.CommandHandler;
import org.dewdrop.fixture.command.DewdropAddFundsToAccountCommand;
import org.dewdrop.fixture.command.DewdropCreateAccountCommand;
import org.dewdrop.fixture.events.DewdropAccountCreated;
import org.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import org.dewdrop.read.readmodel.annotation.EventHandler;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Aggregate
public class DewdropAccountAggregate {
    @AggregateId
    UUID accountId;
    String name;
    BigDecimal balance = BigDecimal.ZERO;

    public DewdropAccountAggregate() {}

    @CommandHandler
    public List<DewdropAccountCreated> handle(DewdropCreateAccountCommand command) {
        if (StringUtils.isEmpty(command.getName())) { throw new IllegalArgumentException("Name cannot be empty"); }
        if (command.getAccountId() == null) { throw new IllegalArgumentException("AccountId cannot be empty"); }
        if (command.getUserId() == null) { throw new IllegalArgumentException("UserId cannot be empty"); }

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
        this.accountId = event.getAccountId();
        this.name = event.getName();
    }

    @EventHandler
    public void on(DewdropFundsAddedToAccount event) {
        // this.accountId = event.getAccountId();
        this.balance = this.balance.add(event.getFunds());
    }
}
