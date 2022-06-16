package com.dewdrop.structure.subscribe;

import static java.util.Objects.requireNonNull;

import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.read.Handler;
import java.util.List;
import lombok.Data;

@Data
public class EventProcessor<T extends Event> {
    private Handler<T> handler;
    private List<Class<?>> messageTypes;

    public EventProcessor(Handler<T> handler, List<Class<?>> messageTypes) {
        requireNonNull(handler, "Handler is required");
        requireNonNull(messageTypes, "messageTypes is required");

        this.handler = handler;
        this.messageTypes = messageTypes;
    }

    public void process(T event) {
        messageTypes.stream().forEach(messageType -> {
            if (event != null && messageType.isAssignableFrom(event.getClass())) {
                T msg = (T) messageType.cast(event);
                handler.handle(msg); // if this throws let it bubble up.
            }
        });
    }

    public boolean isSame(Class<?> messagesType, Object handler) {

        return messageTypes.contains(messagesType) && handler.getClass().equals(this.handler.getClass());
    }

}
