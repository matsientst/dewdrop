package com.dewdrop.structure.read;

import com.dewdrop.structure.api.Message;

public interface Handler<T extends Message> {
    void handle(T event);

    Class<?> getMessageType();
}
