package com.dewdropper.structure.read;

import com.dewdropper.structure.api.Message;

public interface Handler<T extends Message> {
    void handle(T event);

    Class<?> getMessageType();
}
