package com.dewdrop.read.readmodel;

import com.dewdrop.api.result.Result;
import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.api.Message;
import com.dewdrop.utils.QueryHandlerUtils;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class QueryStateOrchestrator {
    private final ReadModelMapper readModelMapper;

    public QueryStateOrchestrator(ReadModelMapper readModelMapper) {
        this.readModelMapper = readModelMapper;
    }

    public <T, R> Result<R> executeQuery(T query) {
        ReadModel<Event> readModel = readModelMapper.getReadModelByQuery(query);
        readModel.updateState();
        return QueryHandlerUtils.callQueryHandler(readModel.getReadModel(), query);
    }
}
