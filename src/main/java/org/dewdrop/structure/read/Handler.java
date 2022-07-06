package org.dewdrop.structure.read;

import org.dewdrop.structure.api.Message;

public interface Handler<T extends Message> {
    void handle(T event);

}
