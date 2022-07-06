package org.dewdrop.fixture.customized;

import org.dewdrop.aggregate.AggregateRoot;
import org.dewdrop.fixture.command.DewdropAddFundsToAccountCommand;
import org.dewdrop.fixture.command.DewdropCreateAccountCommand;
import org.dewdrop.read.readmodel.stream.StreamFactory;
import org.dewdrop.streamstore.process.StandaloneAggregateProcessor;
import org.dewdrop.utils.AggregateIdUtils;
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
