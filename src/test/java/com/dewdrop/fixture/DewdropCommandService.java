package com.dewdrop.fixture;

import com.dewdrop.command.CommandHandler;
import org.apache.commons.lang3.StringUtils;

public class DewdropCommandService {
    @CommandHandler(value = DewdropAccountAggregate.class)
    public DewdropAccountCreated handle(DewdropCreateAccountCommand command) {
        if (StringUtils.isEmpty(command.getName())) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        return new DewdropAccountCreated(command.getAccountId(), command.getName());
    }

    @CommandHandler(value = DewdropAccountAggregate.class)
    public DewdropFundsAddedToAccount handle(DewdropAddFundsToAccountCommand command) {
        if (command.getAccountId() == null) {
            throw new IllegalArgumentException("Id cannot be empty");
        }

        return new DewdropFundsAddedToAccount(command.getAccountId(), command.getFunds());
    }
}
