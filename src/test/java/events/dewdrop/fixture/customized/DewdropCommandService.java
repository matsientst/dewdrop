package events.dewdrop.fixture.customized;

import events.dewdrop.command.CommandHandler;
import events.dewdrop.fixture.automated.DewdropAccountAggregate;
import events.dewdrop.fixture.events.DewdropAccountCreated;
import events.dewdrop.fixture.command.DewdropAddFundsToAccountCommand;
import events.dewdrop.fixture.command.DewdropCreateAccountCommand;
import events.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class DewdropCommandService {
    @CommandHandler(value = DewdropAccountAggregate.class)
    public List<DewdropAccountCreated> handle(DewdropCreateAccountCommand command, DewdropAccountAggregate aggregate) {
        if (StringUtils.isEmpty(command.getName())) { throw new IllegalArgumentException("Name cannot be empty"); }

        return List.of(new DewdropAccountCreated(command.getAccountId(), command.getName(), command.getUserId()));
    }

    @CommandHandler(value = DewdropAccountAggregate.class)
    public List<DewdropFundsAddedToAccount> handle(DewdropAddFundsToAccountCommand command, DewdropAccountAggregate aggregate) {
        if (command.getAccountId() == null) { throw new IllegalArgumentException("Id cannot be empty"); }

        return List.of(new DewdropFundsAddedToAccount(command.getAccountId(), command.getFunds()));
    }
}
