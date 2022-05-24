package com.dewdrop;

import com.dewdrop.api.result.Result;
import com.dewdrop.config.DewdropSettings;
import com.dewdrop.structure.api.Command;

public class Dewdrop {
    private final DewdropSettings settings;

    public Dewdrop(DewdropSettings settings) {
        this.settings = settings;
    }

    public Result<Object> executeCommand(Command command) {
        return settings.getAggregateStateOrchestrator().executeCommand(command);
    }

    public Result<Object> executeSubsequentCommand(Command command, Command previous) {
        return settings.getAggregateStateOrchestrator().executeSubsequentCommand(command, previous);
    }

    public <T, R> Result<R> executeQuery(T query) {
        return settings.getQueryStateOrchestrator().executeQuery(query);
    }

    public DewdropSettings getSettings() {
        return settings;
    }
}
