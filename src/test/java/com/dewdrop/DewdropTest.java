package com.dewdrop;

import com.dewdrop.config.DewdropProperties;
import com.dewdrop.config.DewdropSettings;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DewdropTest {
    @Test
    void test() {
        Dewdrop dewDrop = DewdropSettings.builder()
                .properties(DewdropProperties.builder()
                        .packageToScan("com.dewdrop")
                        .connectionString("esdb://localhost:2113?tls=false")
                        .create())
                .create()
            .start();

        TestCreateAccountCommand command = new TestCreateAccountCommand(UUID.randomUUID(), "test");
        dewDrop.onCommand(command);

        TestAddFundsToAccountCommand addFunds = new TestAddFundsToAccountCommand(command.getAccountId(), new BigDecimal(100));
        dewDrop.onCommand(addFunds);

    }
}
