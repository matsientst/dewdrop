package com.dewdropper;

import java.math.BigDecimal;
import java.util.UUID;

public class TestFundsAddedToAccount extends TestAccountEvent {
    private BigDecimal funds;

    public TestFundsAddedToAccount(UUID accountId, BigDecimal funds) {
        super(accountId);
        this.funds = funds;
    }

    public BigDecimal getFunds() {
        return funds;
    }
}
