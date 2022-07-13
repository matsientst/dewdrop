package events.dewdrop.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import events.dewdrop.fixture.command.DewdropCreateAccountCommand;
import events.dewdrop.fixture.command.DewdropCreateUserCommand;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssignCorrelationAndCausationTest {
    @Test
    @DisplayName("assignTo() - Given an object does it have the field associated with it")
    void assignTo() {
        DewdropCreateUserCommand firstCommand = new DewdropCreateUserCommand(UUID.randomUUID(), "test");
        DewdropCreateAccountCommand secondCommand = new DewdropCreateAccountCommand(UUID.randomUUID(), "Test", UUID.randomUUID());

        secondCommand = AssignCorrelationAndCausation.assignTo(firstCommand, secondCommand);
        assertThat(secondCommand.getCorrelationId(), is(secondCommand.getCorrelationId()));
        assertThat(secondCommand.getCausationId(), is(secondCommand.getCausationId()));
    }
}
