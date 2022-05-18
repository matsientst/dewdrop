package com.dewdrop.command;

import com.dewdrop.api.result.Result;
import com.dewdrop.streamstore.repository.StreamStoreRepository;
import com.dewdrop.structure.api.Command;
import java.util.List;

public interface CommandMapper {
    Result<List<Object>> onCommand(Command command);

    void init(StreamStoreRepository streamStoreRepository);
}
