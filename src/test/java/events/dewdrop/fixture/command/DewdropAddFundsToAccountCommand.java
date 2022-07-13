package events.dewdrop.fixture.command;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class DewdropAddFundsToAccountCommand extends DewdropAccountCommand {
    BigDecimal funds;

    public DewdropAddFundsToAccountCommand(UUID accountId, BigDecimal funds) {
        super(accountId);
        this.funds = funds;
    }
}
