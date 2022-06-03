package com.dewdrop.fixture.automated;

import com.dewdrop.aggregate.annotation.Aggregate;
import com.dewdrop.aggregate.annotation.AggregateId;
import com.dewdrop.command.CommandHandler;
import com.dewdrop.fixture.command.DewdropCreateUserCommand;
import com.dewdrop.fixture.events.DewdropUserCreated;
import com.dewdrop.read.readmodel.annotation.EventHandler;
import java.util.UUID;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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

    @EventHandler
    public void on(DewdropUserCreated userCreated) {
        this.userId = userCreated.getUserId();
        this.username = userCreated.getUsername();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }

        if (o == null || getClass() != o.getClass()) { return false; }

        DewdropUserAggregate that = (DewdropUserAggregate) o;

        return new EqualsBuilder().append(userId, that.userId).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(userId).toHashCode();
    }
}
