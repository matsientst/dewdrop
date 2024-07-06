package events.dewdrop.fixture.readmodel.accountdetails.details;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import events.dewdrop.fixture.events.DewdropAccountCreated;
import events.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import events.dewdrop.fixture.events.DewdropUserCreated;
import events.dewdrop.fixture.events.UserLoggedIn;
import events.dewdrop.read.readmodel.annotation.EventHandler;
import events.dewdrop.read.readmodel.annotation.ForeignCacheKey;
import events.dewdrop.read.readmodel.annotation.PrimaryCacheKey;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Data
@NoArgsConstructor
@Log4j2
public class DewdropAccountDetails {
    @PrimaryCacheKey(creationEvent = DewdropAccountCreated.class)
    private UUID accountId;
    private String name;
    private BigDecimal balance = BigDecimal.ZERO;
    @ForeignCacheKey(eventKeyField = "userId")
    private UUID userId;
    private String username;
    private UUID causationId;
    private LocalDateTime lastLogin;

    @EventHandler
    public void on(DewdropAccountCreated event) {
        this.causationId = event.getCausationId();
        this.accountId = event.getAccountId();
        this.name = event.getName();
        this.userId = event.getUserId();
    }

    @EventHandler
    public void on(DewdropFundsAddedToAccount event) {
        log.info("processing DewdropFundsAddedToAccount:{}", event);
        this.balance = this.balance.add(event.getFunds());
    }

    @EventHandler
    public void on(DewdropUserCreated userCreated) {
        log.info("processing DewdropUserCreated:{}", userCreated);
        this.username = userCreated.getUsername();
    }

    @EventHandler
    public void on(UserLoggedIn event) {
        log.info("processing UserLoggedIn:{}", event);
        this.lastLogin = event.getLogin();
    }
}
