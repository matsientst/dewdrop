package com.dewdrop.fixture;

import com.dewdrop.aggregate.Aggregate;
import com.dewdrop.aggregate.AggregateId;
import com.dewdrop.command.CommandHandler;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

@Aggregate
public class DewdropAccountAggregate {
    @AggregateId
    UUID accountId;
    String name;
    BigDecimal balance = BigDecimal.ZERO;

    public DewdropAccountAggregate() {}
    @CommandHandler
    public List<DewdropAccountCreated> handle(DewdropCreateAccountCommand command) {
        if (StringUtils.isEmpty(command.getName())) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        return List.of(new DewdropAccountCreated(command.getAccountId(), command.getName()));
    }
    @CommandHandler
    public List<DewdropFundsAddedToAccount> handle(DewdropAddFundsToAccountCommand command) {
        if (command.getAccountId() == null) {
            throw new IllegalArgumentException("Id cannot be empty");
        }

        DewdropFundsAddedToAccount dewdropFundsAddedToAccount = new DewdropFundsAddedToAccount(command.getAccountId(), command.getFunds());
        return List.of(dewdropFundsAddedToAccount);
    }

    public void on(DewdropAccountCreated event) {
        this.accountId = event.getAccountId();
        this.name = event.getName();
    }

    public void on(DewdropFundsAddedToAccount event) {
        this.balance = this.balance.add(event.getFunds());
    }
}
