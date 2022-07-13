package events.dewdrop.structure.read;

import events.dewdrop.structure.api.Message;

public interface Handler<T extends Message> {
    void handle(T event);

}
