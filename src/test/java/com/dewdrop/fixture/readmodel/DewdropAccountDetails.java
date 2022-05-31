package com.dewdrop.fixture.readmodel;

import com.dewdrop.fixture.events.DewdropAccountCreated;
import com.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import com.dewdrop.fixture.events.DewdropUserCreated;
import com.dewdrop.read.readmodel.annotation.ForeignCacheKey;
import com.dewdrop.read.readmodel.annotation.PrimaryCacheKey;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Data
@NoArgsConstructor
@Log4j2
public class DewdropAccountDetails {
    @PrimaryCacheKey
    private UUID accountId;
    private String name;
    private BigDecimal balance = BigDecimal.ZERO;
    @ForeignCacheKey
    private UUID userId;
    private String username;

    public void on(DewdropAccountCreated event) {
        this.accountId = event.getAccountId();
        this.name = event.getName();
        this.userId = event.getUserId();
    }

    public void on(DewdropFundsAddedToAccount event) {
        this.balance = this.balance.add(event.getFunds());
    }

    public void on(DewdropUserCreated userCreated) {
        this.username = userCreated.getUsername();
    }
}
