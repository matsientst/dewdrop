package com.dewdrop.read.readmodel;

import com.dewdrop.read.readmodel.stream.Stream;
import com.dewdrop.structure.api.Message;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public abstract class AbstractReadModel {
    protected List<Stream<? super Message>> streams = new ArrayList<>();

    protected AbstractReadModel() {
    }

    public abstract <T extends Message> void handle(T event);

    public void addStream(Stream stream) {
        this.streams.add(stream);
    }


    public void updateState() {
        streams.forEach(stream -> stream.updateState());
    }
}
