package com.dewdrop.structure.api;

import java.util.UUID;
import lombok.Data;

@Data
public abstract class Event implements Message {
    private UUID correlationId;
    private UUID causationId;

}