package com.dewdropper.aggregate;

import com.dewdropper.TestCreateAccountCommand;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class AggregateStateOrchestratorTest {
    @Test
    @Disabled
    void create() {
        AggregateStateOrchestrator orchestrator = new AggregateStateOrchestrator();
        TestCreateAccountCommand command = new TestCreateAccountCommand(UUID.randomUUID(),"test");
        orchestrator.onCommand(command);
    }


}
