package com.dewdrop.read.readmodel;

import com.dewdrop.api.result.Result;
import com.dewdrop.command.CommandMapper;
import com.dewdrop.structure.api.Command;
import java.util.List;

public class ReadModelOrchestrator {

    private ReadModelMapper readModelMapper;

    public ReadModelOrchestrator() {}

    public ReadModelOrchestrator(ReadModelMapper readModelMapper) {
        this.readModelMapper = readModelMapper;
    }

}
