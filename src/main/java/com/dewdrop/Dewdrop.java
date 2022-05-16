package com.dewdrop;

import com.dewdrop.api.result.Result;
import com.dewdrop.config.DewdropSettings;
import com.dewdrop.structure.api.Command;
import java.util.List;


public class Dewdrop {
    private DewdropSettings settings;

    public Dewdrop(DewdropSettings settings) {
        this.settings = settings;
    }

    public Result<List<Object>> onCommand(Command command) {
        return settings.getAggregateStateOrchestrator()
            .onCommand(command);
    }

}
