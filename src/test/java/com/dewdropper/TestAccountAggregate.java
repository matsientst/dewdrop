package com.dewdropper;

import com.dewdropper.aggregate.Aggregate;
import com.dewdropper.aggregate.AggregateId;
import java.math.BigDecimal;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

@Aggregate
public class TestAccountAggregate {
    @AggregateId
    UUID accountId;
    String name;
    BigDecimal balance = BigDecimal.ZERO;

    public TestAccountAggregate() {}

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


    public TestAccountCreated handle(TestCreateAccountCommand command) {
        if (StringUtils.isEmpty(command.getName())) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        return new TestAccountCreated(command.getAccountId(), command.getName());
    }

    public TestFundsAddedToAccount handle(TestAddFundsToAccountCommand command) {
        if (command.getAccountId() == null) {
            throw new IllegalArgumentException("Id cannot be empty");
        }

        return new TestFundsAddedToAccount(command.getAccountId(), command.getFunds());
    }

    public void on(TestAccountCreated event) {
        this.accountId = event.getAccountId();
        this.name = event.getName();
    }

    public void on(TestFundsAddedToAccount event) {
        this.balance = this.balance.add(event.getFunds());
    }
}
