package com.dewdrop.fixture;

import com.dewdrop.aggregate.Aggregate;
import com.dewdrop.aggregate.AggregateId;
import com.dewdrop.command.CommandHandler;
import java.util.UUID;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Aggregate
@Data
public class DewdropUserAggregate {
    @AggregateId
    UUID userId;
    private String username;

    public DewdropUserAggregate() {
    }

    @CommandHandler
    public UserCreated handle(CreateUserCommand command) {
        if (StringUtils.isEmpty(command.getUsername())) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (command.getUserId() == null) {
            throw new IllegalArgumentException("UserId cannot be empty");
        }

        return new UserCreated(command.getUserId(), command.getUsername());
    }

    public void on(UserCreated userCreated) {
        this.userId = userCreated.getUserId();
        this.username = userCreated.getUsername();
    }
}
