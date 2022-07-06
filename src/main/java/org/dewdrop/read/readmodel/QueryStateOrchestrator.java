package org.dewdrop.read.readmodel;

import org.dewdrop.api.result.Result;
import org.dewdrop.structure.api.Event;
import org.dewdrop.utils.QueryHandlerUtils;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;

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
        readModel.updateState();
        return QueryHandlerUtils.callQueryHandler(readModel.getReadModelWrapper(), query);
    }
}
