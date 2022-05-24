package com.dewdrop.fixture;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DewdropFundsAddedToAccount extends DewdropAccountEvent {
    private BigDecimal funds;

    public DewdropFundsAddedToAccount(UUID accountId, BigDecimal funds) {
        super(accountId);
        this.funds = funds;
    }

    public BigDecimal getFunds() {
        return funds;
    }
}
