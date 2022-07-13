package events.dewdrop.structure.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;


public class AbstractMessage implements Message {
    @JsonIgnore
    private UUID messageId;

    public AbstractMessage() {
        this.messageId = UUID.randomUUID();
    }

    @Override
    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }
}
