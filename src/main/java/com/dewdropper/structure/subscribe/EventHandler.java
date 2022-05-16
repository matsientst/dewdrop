package com.dewdropper.structure.subscribe;

import static java.util.Objects.requireNonNull;

import com.dewdropper.structure.api.Message;
import com.dewdropper.structure.read.Handler;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class EventHandler<T extends Message> {
    private Handler<T> handler;
    private String handlerName;
    private Class<?> messageType;

    public EventHandler(Handler<T> handler, String handlerName) {
        requireNonNull("Handler is required");

        this.handler = handler;
        this.handlerName = StringUtils.isEmpty(handlerName) ? "" : handlerName;
        this.messageType = handler.getMessageType();
    }

    public boolean tryHandle(T event) {
        if (event != null && messageType.isAssignableFrom(event.getClass())) {
            T msg = (T) messageType.cast(event);
            handler.handle(msg); // if this throws let it bubble up.
            return true;
        }
        return false;
    }

    public boolean isSame(Class<?> messagesType, Object handler) {
        return messagesType.equals(this.messageType) && handler.getClass().equals(this.handler.getClass());
    }

}
