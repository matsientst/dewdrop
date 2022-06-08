package com.dewdrop.fixture.customized;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.fixture.command.DewdropAddFundsToAccountCommand;
import com.dewdrop.fixture.command.DewdropCreateAccountCommand;
import com.dewdrop.read.readmodel.StreamFactory;
import com.dewdrop.streamstore.process.StandaloneAggregateProcessor;
import com.dewdrop.utils.AggregateIdUtils;
import java.util.Optional;
import java.util.UUID;

public class DewdropStandaloneCommandService {
    private StandaloneAggregateProcessor standaloneAggregateProcessor;
    private StreamFactory streamFactory;

    public DewdropStandaloneCommandService(StandaloneAggregateProcessor standaloneAggregateProcessor) {
        this.standaloneAggregateProcessor = standaloneAggregateProcessor;
    }

    public DewdropAccountAggregateSubclass process(DewdropCreateAccountCommand command) {
        DewdropAccountAggregateSubclass accountAggregate = new DewdropAccountAggregateSubclass();
        accountAggregate.handle(command);
        standaloneAggregateProcessor.save(accountAggregate);
        return accountAggregate;
    }

    public DewdropAccountAggregateSubclass process(DewdropAddFundsToAccountCommand command) {
        DewdropAccountAggregateSubclass accountAggregate = new DewdropAccountAggregateSubclass();

        Optional<UUID> optId = AggregateIdUtils.getAggregateId(command);
        if (optId.isEmpty()) { return accountAggregate; }

        UUID id = optId.get();

        AggregateRoot aggregateRoot = standaloneAggregateProcessor.getById(accountAggregate, id);
        accountAggregate.handle(command);
        standaloneAggregateProcessor.save(aggregateRoot);
        return accountAggregate;
    }
}
