package com.dewdrop.read.readmodel;

import com.dewdrop.structure.api.Event;
import java.util.Optional;

public interface ReadModelMapper {

    Optional<ReadModel<Event>> getReadModelByQuery(Object query);

    void init(ReadModelFactory readModelFactory);
}
