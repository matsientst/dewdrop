package events.dewdrop.read.readmodel;

import events.dewdrop.api.result.Result;
import events.dewdrop.structure.api.Event;
import events.dewdrop.utils.AggregateIdUtils;
import events.dewdrop.utils.QueryHandlerUtils;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;
import java.util.UUID;

@Log4j2
public class QueryStateOrchestrator {
    private final ReadModelMapper readModelMapper;

    public QueryStateOrchestrator(ReadModelMapper readModelMapper) {
        this.readModelMapper = readModelMapper;
    }

    public <T, R> Result<R> executeQuery(T query) {
        Optional<ReadModel<Event>> optReadModel = readModelMapper.getReadModelByQuery(query);
        if (optReadModel.isEmpty()) {
            log.error("no read model found for query: {}", query.getClass().getSimpleName());
            return Result.ofException(new IllegalStateException("no read model found for query: " + query.getClass().getSimpleName()));
        }
        ReadModel<Event> readModel = optReadModel.get();
        log.info("Querying read model: {} with QueryType: {}", readModel.getClass().getSimpleName(), query.getClass().getSimpleName());

        Optional<UUID> aggregateId = AggregateIdUtils.hasAggregateId(query) ? AggregateIdUtils.getAggregateId(query) : Optional.empty();
        readModel.updateQueryState(aggregateId);
        return QueryHandlerUtils.callQueryHandler(readModel.getReadModelWrapper(), query);
    }
}
