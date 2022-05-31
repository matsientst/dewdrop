package com.dewdrop.fixture.automated;

import com.dewdrop.aggregate.Aggregate;
import com.dewdrop.aggregate.AggregateId;
import com.dewdrop.command.CommandHandler;
import com.dewdrop.fixture.command.DewdropCreateUserCommand;
import com.dewdrop.fixture.events.DewdropUserCreated;
import java.util.UUID;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Aggregate
@Data
public class DewdropUserAggregate {
    @AggregateId
    UUID userId;
    private String username;

    public DewdropUserAggregate() {}

    @CommandHandler
    public DewdropUserCreated handle(DewdropCreateUserCommand command) {
        if (StringUtils.isEmpty(command.getUsername())) { throw new IllegalArgumentException("Username cannot be empty"); }
        if (command.getUserId() == null) { throw new IllegalArgumentException("UserId cannot be empty"); }

        return new DewdropUserCreated(command.getUserId(), command.getUsername());
    }

    public void on(DewdropUserCreated userCreated) {
        this.userId = userCreated.getUserId();
        this.username = userCreated.getUsername();
    }
}
