package com.dewdropper;

import com.dewdropper.api.DefaultCommand;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class TestAddFundsToAccountCommand extends TestAccountCommand {
    BigDecimal funds;

    public TestAddFundsToAccountCommand(UUID accountId, BigDecimal funds) {
        super(accountId);
        this.funds = funds;
    }
}
