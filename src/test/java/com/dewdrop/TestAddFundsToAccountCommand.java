package com.dewdrop;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class TestAddFundsToAccountCommand extends TestAccountCommand {
    BigDecimal funds;

    public TestAddFundsToAccountCommand(UUID accountId, BigDecimal funds) {
        super(accountId);
        this.funds = funds;
    }
}
