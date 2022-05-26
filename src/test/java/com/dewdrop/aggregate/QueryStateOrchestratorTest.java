package com.dewdrop.aggregate;

import com.dewdrop.fixture.command.DewdropCreateAccountCommand;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class QueryStateOrchestratorTest {
    @Test
    @Disabled
    void create() {
        AggregateStateOrchestrator orchestrator = new AggregateStateOrchestrator();
        DewdropCreateAccountCommand command = new DewdropCreateAccountCommand(UUID.randomUUID(), "test", UUID.randomUUID());
        orchestrator.executeCommand(command);
    }


}
