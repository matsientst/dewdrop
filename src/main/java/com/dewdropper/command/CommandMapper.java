package com.dewdropper.command;

import com.dewdropper.api.result.Result;
import com.dewdropper.structure.api.Command;
import java.util.List;

public interface CommandMapper {
    Result<List<Object>> onCommand(Command command);
}
