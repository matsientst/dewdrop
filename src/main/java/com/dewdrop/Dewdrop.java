package com.dewdrop;

import com.dewdrop.api.result.Result;
import com.dewdrop.config.DewdropSettings;
import com.dewdrop.structure.api.Command;
import com.dewdrop.structure.api.Message;
import java.util.List;

public class Dewdrop {
    private final DewdropSettings settings;

    public Dewdrop(DewdropSettings settings) {
        this.settings = settings;
    }

    public Result<Object> onCommand(Command command) {
        return settings.getAggregateStateOrchestrator()
            .onCommand(command);
    }

    public Result<Object> onSubsequentCommand(Command command, Command previous) {
        return settings.getAggregateStateOrchestrator()
            .onSubsequentCommand(command, previous);
    }

    public <T, R> Result<R> onQuery(T query) {
        return settings.getQueryStateOrchestrator()
            .onQuery(query);
    }

    public DewdropSettings getSettings() {
        return settings;
    }
}
