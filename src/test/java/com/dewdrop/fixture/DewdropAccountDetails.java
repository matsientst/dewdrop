package com.dewdrop.fixture;

import com.dewdrop.read.readmodel.annotation.AlternateCacheKey;
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
    @AlternateCacheKey
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

    public void on(UserCreated userCreated) {
        log.info("UserCreated accountId:{}, userId:{}, new userId:{}", accountId, userId, userCreated.getUserId());
        this.username = userCreated.getUsername();
    }
}
