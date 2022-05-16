package com.dewdrop.read.events;

import com.dewdrop.structure.api.Message;
import com.dewdrop.streamstore.subscribe.Subscription;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class ReadModel<T extends Message> {
    protected Subscription<T> subscription;


    // public void readAndSubscribe(StreamType streamType, String category, Consumer<Event> consumer,
    // Class<?> messageType) {
    // NameAndPosition nameAndPosition = NameAndPosition.builder()
    // .streamType(streamType)
    // .category(category)
    // .consumer(consumer)
    // .messageType(messageType)
    // .create();
    // try {
    // nameAndPosition = getNameAndPosition(nameAndPosition);
    // subscribeByNameAndPosition(nameAndPosition);
    // } catch (MissingStreamException e) {
    // pollForCompletion(nameAndPosition);
    // }
    // }
}
