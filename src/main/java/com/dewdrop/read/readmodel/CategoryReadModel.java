package com.dewdrop.read.readmodel;

import com.dewdrop.read.StreamDetails;
import com.dewdrop.read.StreamType;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.read.Direction;
import java.util.function.Consumer;
import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class CategoryReadModel extends AbstractReadModel  {
    private String category;

    protected CategoryReadModel() {
        super();
    }

    public String getCategory() {
        return category;
    }

    public <T extends Message> Consumer<T> handler() {
        return message -> process((T) message);
    }

    abstract <T extends Message> void process(T message);

}
