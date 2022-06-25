package com.dewdrop.fixture.readmodel.accountdetails.summary;

import com.dewdrop.fixture.events.DewdropAccountCreated;
import com.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import com.dewdrop.read.readmodel.annotation.EventHandler;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Log4j2
public class DewdropAccountSummary {
    private int countOfAccounts = 0;
    private BigDecimal totalFunds = new BigDecimal(0);

    @EventHandler
    public void on(DewdropFundsAddedToAccount event) {
        log.info("=====================> Adding funds to account:{}, funds:{}", event.getAccountId(), event.getFunds());
        this.totalFunds = totalFunds.add(event.getFunds());
    }

    @EventHandler
    public void on(DewdropAccountCreated event) {
        this.countOfAccounts++;
    }

}
