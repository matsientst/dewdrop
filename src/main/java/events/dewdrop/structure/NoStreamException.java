package events.dewdrop.structure;

import lombok.Data;

@Data
public class NoStreamException extends RuntimeException {
    private String stream;

    public NoStreamException(String stream) {
        super(stream);
        this.stream = stream;
    }
}

