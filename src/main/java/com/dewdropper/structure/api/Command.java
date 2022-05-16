package com.dewdropper.structure.api;

import com.dewdropper.structure.events.CorrelationCausation;
import java.util.UUID;

public interface Command extends CorrelationCausation, Message {

}
