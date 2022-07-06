package org.dewdrop.fixture.events;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
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
