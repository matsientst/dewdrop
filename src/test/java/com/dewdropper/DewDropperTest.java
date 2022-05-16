package com.dewdropper;

import com.dewdropper.config.DewDropperProperties;
import com.dewdropper.config.DewDropperSettings;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DewDropperTest {
    @Test
    void test() {
        DewDropper dewDropper = DewDropperSettings.builder()
                .properties(DewDropperProperties.builder()
                        .connectionString("esdb://localhost:2113?tls=false")
                        .streamPrefix("")
                        .create())
                .create()
            .start();

        TestCreateAccountCommand command = new TestCreateAccountCommand(UUID.randomUUID(), "test");
        dewDropper.onCommand(command);

        TestAddFundsToAccountCommand addFunds = new TestAddFundsToAccountCommand(command.getAccountId(), new BigDecimal(100));
        dewDropper.onCommand(addFunds);

    }
}
