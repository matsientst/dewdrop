package events.dewdrop.read.readmodel.cache;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.spy;

import events.dewdrop.fixture.events.DewdropAccountCreated;
import events.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import events.dewdrop.fixture.events.DewdropUserCreated;
import events.dewdrop.fixture.events.UserLoggedIn;
import events.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImprovedMapBackedInMemoryCacheProcessorTest {
    private ImprovedMapBackedInMemoryCacheProcessor<DewdropAccountDetails> sut;
    private Map<UUID, DewdropAccountDetails> cache;

    UUID accountId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    DewdropAccountCreated accountCreated = new DewdropAccountCreated(accountId, "test", userId);
    DewdropUserCreated userCreated = new DewdropUserCreated(userId, "tester guy");

    @BeforeEach
    void setup() {
        sut = spy(new ImprovedMapBackedInMemoryCacheProcessor<>(DewdropAccountDetails.class));
    }

    @Test
    @DisplayName("constructor() - confirm that we construct properly")
    void constructor() {
        assertThat(sut.getCachedStateObjectType(), is(DewdropAccountDetails.class));
        assertThat(sut.getCache().size(), is(0));
    }

    @Test
    @DisplayName("process() - Confirm primary creation event is processed")
    void process() {
        sut.process(accountCreated);
        DewdropAccountDetails result = sut.getCache().get(accountId);
        assertThat(result.getAccountId(), is(accountId));
    }

    @Test
    @DisplayName("process() - Confirm primary creation event is processed out of order")
    void process_outOfOrder() {
        DewdropFundsAddedToAccount message = new DewdropFundsAddedToAccount(accountId, new BigDecimal(34));
        sut.process(message);
        sut.process(accountCreated);
        DewdropAccountDetails result = sut.getCache().get(accountId);
        assertThat(result.getAccountId(), is(accountId));
        assertThat(result.getBalance(), is(message.getFunds()));
    }

    @Test
    @DisplayName("process() - Confirm foreign creation event is processed")
    void process_foreignEvent() {
        sut.process(accountCreated);
        sut.process(userCreated);
        DewdropAccountDetails result = sut.getCache().get(accountId);
        assertThat(result.getUsername(), is(userCreated.getUsername()));
    }

    @Test
    @DisplayName("process() - Confirm foreign creation event is processed,  even if it came first")
    void process_foreignEvent_tooSoon() {
        sut.process(userCreated);
        sut.process(accountCreated);
        DewdropAccountDetails result = sut.getCache().get(accountId);
        assertThat(result.getUsername(), is(userCreated.getUsername()));
        assertThat(sut.getForeignStashedMessages().get(userId).isEmpty(), is(true));
    }

    @Test
    @DisplayName("process() - Confirm foreign creation event is processed,  even if it came first")
    void process_foreignEvent_tooSoon_withMultiples() {
        UserLoggedIn login = new UserLoggedIn(userId, LocalDateTime.now());
        sut.process(userCreated);
        sut.process(login);
        sut.process(accountCreated);
        DewdropAccountDetails result = sut.getCache().get(accountId);
        assertThat(result.getLastLogin(), is(login.getLogin()));
        assertThat(sut.getForeignStashedMessages().get(userId).isEmpty(), is(true));
    }

}
