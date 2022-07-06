package org.dewdrop.read.readmodel;

import org.dewdrop.structure.api.Event;
import java.util.Optional;

public interface ReadModelMapper {

    Optional<ReadModel<Event>> getReadModelByQuery(Object query);

    void init(ReadModelFactory readModelFactory);
}
