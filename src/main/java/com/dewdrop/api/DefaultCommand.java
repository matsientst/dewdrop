package com.dewdrop.api;

import com.dewdrop.structure.api.Command;
import java.util.UUID;
import lombok.Data;

@Data
public class DefaultCommand implements Command {
    protected UUID correlationId;
    protected UUID causationId;
}
