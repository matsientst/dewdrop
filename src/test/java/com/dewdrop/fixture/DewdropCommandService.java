package com.dewdrop.fixture;

import com.dewdrop.command.CommandHandler;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class DewdropCommandService {
    // @CommandHandler(value = DewdropAccountAggregate.class)
    public List<DewdropAccountCreated> handle(DewdropCreateAccountCommand command, DewdropAccountAggregate aggregate) {
        if (StringUtils.isEmpty(command.getName())) { throw new IllegalArgumentException("Name cannot be empty"); }

        return List.of(new DewdropAccountCreated(command.getAccountId(), command.getName(), command.getUserId()));
    }

    // @CommandHandler(value = DewdropAccountAggregate.class)
    public List<DewdropFundsAddedToAccount> handle(DewdropAddFundsToAccountCommand command, DewdropAccountAggregate aggregate) {
        if (command.getAccountId() == null) { throw new IllegalArgumentException("Id cannot be empty"); }

        return List.of(new DewdropFundsAddedToAccount(command.getAccountId(), command.getFunds()));
    }
}
