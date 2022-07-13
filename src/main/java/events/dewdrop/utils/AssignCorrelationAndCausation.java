package events.dewdrop.utils;

import events.dewdrop.structure.events.CorrelationCausation;

public class AssignCorrelationAndCausation {
    private AssignCorrelationAndCausation() {}

    public static <T extends CorrelationCausation> T assignTo(CorrelationCausation previous, T command) {
        command.setCausationId(previous.getMessageId());
        command.setCorrelationId(previous.getCorrelationId());
        return command;
    }

}
