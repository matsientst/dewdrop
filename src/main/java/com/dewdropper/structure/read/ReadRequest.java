package com.dewdropper.structure.read;

import java.util.Optional;
import lombok.Data;

@Data

public class ReadRequest {
    String stream;
    Long start;
    Long count;
    Direction direction;

    public ReadRequest(String stream, Long start, Long count, Direction direction) {
        this.stream = stream;
        this.count = Optional.ofNullable(count).orElse(Long.MAX_VALUE);
        this.start = start;
        this.direction = direction;
    }
}
