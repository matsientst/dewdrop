package com.dewdrop.fixture;

import com.dewdrop.aggregate.Aggregate;
import com.dewdrop.aggregate.AggregateId;
import java.math.BigDecimal;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

@Aggregate
public class DewdropAccountAggregate {
    @AggregateId
    UUID accountId;
    String name;
    BigDecimal balance = BigDecimal.ZERO;

    public DewdropAccountAggregate() {}

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public DewdropAccountCreated handle(DewdropCreateAccountCommand command) {
        if (StringUtils.isEmpty(command.getName())) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        return new DewdropAccountCreated(command.getAccountId(), command.getName());
    }

    public DewdropFundsAddedToAccount handle(DewdropAddFundsToAccountCommand command) {
        if (command.getAccountId() == null) {
            throw new IllegalArgumentException("Id cannot be empty");
        }

        return new DewdropFundsAddedToAccount(command.getAccountId(), command.getFunds());
    }

    public void on(DewdropAccountCreated event) {
        this.accountId = event.getAccountId();
        this.name = event.getName();
    }

    public void on(DewdropFundsAddedToAccount event) {
        this.balance = this.balance.add(event.getFunds());
    }
}
