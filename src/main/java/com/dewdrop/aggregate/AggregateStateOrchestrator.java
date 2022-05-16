package com.dewdrop.aggregate;

import com.dewdrop.api.result.Result;
import com.dewdrop.command.CommandMapper;
import com.dewdrop.structure.api.Command;
import java.util.List;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AggregateStateOrchestrator {
    private CommandMapper commandMapper;

    public AggregateStateOrchestrator() {}

    public AggregateStateOrchestrator(CommandMapper commandMapper) {
        this.commandMapper = commandMapper;
    }

    public Result<List<Object>> onCommand(Command command) {
        return commandMapper.onCommand(command);
    }
}
