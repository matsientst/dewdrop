package com.dewdrop.utils;

import com.dewdrop.structure.api.Command;
import com.dewdrop.structure.events.CorrelationCausation;
import java.util.UUID;

public class AssignCorrelationAndCausation {
    private UUID correlationId;
    private UUID causationId;

    AssignCorrelationAndCausation() {}

    AssignCorrelationAndCausation(CorrelationCausation message) {
        this.correlationId = message.getCorrelationId();
        this.causationId = message.getCorrelationId();
    }

    public static <T extends CorrelationCausation> AssignCorrelationAndCausation previous(T message) {
        AssignCorrelationAndCausation causation = new AssignCorrelationAndCausation(message);
        return causation;
    }

    public static Command assignTo(CorrelationCausation previous, Command command) {
        command.setCausationId(previous.getMessageId());
        command.setCorrelationId(previous.getCorrelationId());
        return command;
    }

    public  <T extends CorrelationCausation> T nextCommand(T message) {
        message.setCorrelationId(message.getCorrelationId());
        message.setCausationId(causationId == null ? null : causationId);
        return message;
    }

    public static <T extends CorrelationCausation> T firstCommand(T command) {
        return new AssignCorrelationAndCausation(command).nextCommand(command);
    }
}
