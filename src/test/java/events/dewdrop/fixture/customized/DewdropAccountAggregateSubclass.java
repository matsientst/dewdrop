package events.dewdrop.fixture.customized;

import events.dewdrop.aggregate.AggregateRoot;
import events.dewdrop.aggregate.annotation.AggregateId;
import events.dewdrop.fixture.events.DewdropAccountCreated;
import events.dewdrop.fixture.command.DewdropAddFundsToAccountCommand;
import events.dewdrop.fixture.command.DewdropCreateAccountCommand;
import events.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import java.math.BigDecimal;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

public class DewdropAccountAggregateSubclass extends AggregateRoot {

    @AggregateId
    UUID accountId;
    String name;
    BigDecimal balance = BigDecimal.ZERO;

    public DewdropAccountAggregateSubclass() {}

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
        if (StringUtils.isEmpty(command.getName())) { throw new IllegalArgumentException("Name cannot be empty"); }

        DewdropAccountCreated testAccountCreated = new DewdropAccountCreated(command.getAccountId(), command.getName(), command.getUserId());
        raise(testAccountCreated);
        return testAccountCreated;
    }

    public DewdropFundsAddedToAccount handle(DewdropAddFundsToAccountCommand command) {
        if (command.getAccountId() == null) { throw new IllegalArgumentException("Id cannot be empty"); }

        DewdropFundsAddedToAccount testFundsAddedToAccount = new DewdropFundsAddedToAccount(command.getAccountId(), command.getFunds());
        raise(testFundsAddedToAccount);
        return testFundsAddedToAccount;
    }

    public void on(DewdropAccountCreated event) {
        this.accountId = event.getAccountId();
        this.name = event.getName();
    }

    public void on(DewdropFundsAddedToAccount event) {
        this.balance = this.balance.add(event.getFunds());
    }
}
