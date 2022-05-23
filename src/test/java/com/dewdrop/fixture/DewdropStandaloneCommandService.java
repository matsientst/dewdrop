package com.dewdrop.fixture;

import com.dewdrop.aggregate.AggregateRoot;
import com.dewdrop.read.StreamDetails;
import com.dewdrop.read.readmodel.StreamDetailsFactory;
import com.dewdrop.streamstore.repository.StreamStoreGetByIDRequest;
import com.dewdrop.streamstore.repository.StreamStoreRepository;
import com.dewdrop.utils.AggregateIdUtils;
import java.util.Optional;
import java.util.UUID;

public class DewdropStandaloneCommandService {
    private StreamStoreRepository streamStoreRepository;
    private StreamDetailsFactory streamDetailsFactory;

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

        Optional<UUID> optId = AggregateIdUtils.getAggregateId(command);
        if (optId.isEmpty()) {
            return accountAggregate;
        }

        UUID id = optId.get();
        StreamDetails streamDetails = streamDetailsFactory.fromAggregateRoot(accountAggregate, id);

        StreamStoreGetByIDRequest request = StreamStoreGetByIDRequest.builder()
            .streamDetails(streamDetails)
            .aggregateRoot(accountAggregate)
            .id(id)
            .command(command)
            .create();

        AggregateRoot aggregateRoot = streamStoreRepository.getById(request);
        accountAggregate.handle(command);
        streamStoreRepository.save(aggregateRoot);
        return accountAggregate;
    }
}
