package com.dewdrop.read.readmodel;

import com.dewdrop.api.result.Result;
import com.dewdrop.utils.DewdropReflectionUtils;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class QueryStateOrchestrator {
    private ReadModelMapper readModelMapper;

    public QueryStateOrchestrator() {}

    public QueryStateOrchestrator(ReadModelMapper readModelMapper) {
        this.readModelMapper = readModelMapper;
    }

    public <T, R> Result<R> executeQuery(T query) {
        CacheableReadModel<Object> readModel = readModelMapper.getReadModelByQuery(query);
        readModel.updateState();
        Optional<Result<?>> handle = DewdropReflectionUtils.callMethod(readModel.getReadModel(), "handle", query, readModel.getCachedItems());
        if (handle.isPresent()) {
            return (Result<R>) handle.get();
        }
        return Result.empty();
    }
}
