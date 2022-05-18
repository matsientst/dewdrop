package com.dewdrop.fixture;

import java.math.BigDecimal;
import java.util.UUID;

public class DewdropFundsAddedToAccount extends DewdropAccountEvent {
    private BigDecimal funds;
    public DewdropFundsAddedToAccount(){}
    public DewdropFundsAddedToAccount(UUID accountId, BigDecimal funds) {
        super(accountId);
        this.funds = funds;
    }

    public BigDecimal getFunds() {
        return funds;
    }
}
