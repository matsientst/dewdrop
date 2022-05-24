package com.dewdrop.structure.api;

import com.dewdrop.structure.events.CorrelationCausation;
import java.util.UUID;
import lombok.Data;

@Data
public abstract class Event extends CorrelationCausation {
    public Event() {}
}
