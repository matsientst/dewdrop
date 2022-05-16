package com.dewdrop.structure.events;

import java.util.UUID;

public interface CorrelationCausation {
    UUID getCorrelationId();

    void setCorrelationId(UUID var1);

    UUID getCausationId();

    void setCausationId(UUID var1);
}
