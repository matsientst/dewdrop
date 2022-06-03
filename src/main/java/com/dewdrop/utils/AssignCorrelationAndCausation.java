package com.dewdrop.utils;

import com.dewdrop.structure.api.Command;
import com.dewdrop.structure.events.CorrelationCausation;
import java.util.UUID;

public class AssignCorrelationAndCausation {
    private AssignCorrelationAndCausation() {}

    public static <T extends CorrelationCausation> T assignTo(CorrelationCausation previous, T command) {
        command.setCausationId(previous.getMessageId());
        command.setCorrelationId(previous.getCorrelationId());
        return command;
    }

}
