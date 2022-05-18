package com.dewdrop.fixture;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.streamstore.repository.StreamStoreRepository;
import java.util.Optional;

public class DewdropStandaloneCommandService {
    private StreamStoreRepository streamStoreRepository;

    public DewdropStandaloneCommandService(StreamStoreRepository streamStoreRepository) {
        this.streamStoreRepository = streamStoreRepository;
    }

    public DewdropAccountAggregateSubclass process(DewdropCreateAccountCommand command) {
        DewdropAccountAggregateSubclass accountAggregate = new DewdropAccountAggregateSubclass();
        accountAggregate.handle(command);
        streamStoreRepository.save(accountAggregate);
        return accountAggregate;
    }

    public DewdropAccountAggregateSubclass process(DewdropAddFundsToAccountCommand command) {
        DewdropAccountAggregateSubclass accountAggregate = new DewdropAccountAggregateSubclass();
        Optional<AggregateRoot> aggregateRoot = streamStoreRepository.getById(command.getAccountId(), accountAggregate, Integer.MAX_VALUE, command);
        if (aggregateRoot.isPresent()) {
            accountAggregate = (DewdropAccountAggregateSubclass) aggregateRoot.get();
            accountAggregate.handle(command);
            streamStoreRepository.save(accountAggregate);
            return accountAggregate;
        }
        return null;
    }
}
