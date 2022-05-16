package com.dewdropper;

import com.dewdropper.api.result.Result;
import com.dewdropper.config.DewDropperSettings;
import com.dewdropper.structure.api.Command;
import java.util.List;


public class DewDropper {
    private DewDropperSettings settings;

    public DewDropper(DewDropperSettings settings) {
        this.settings = settings;
    }

    public Result<List<Object>> onCommand(Command command) {
        return settings.getAggregateStateOrchestrator()
            .onCommand(command);
    }

}
