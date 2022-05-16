package com.dewdropper.aggregate;

import com.dewdropper.api.result.Result;
import com.dewdropper.command.CommandMapper;
import com.dewdropper.structure.api.Command;
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
