package org.dewdrop.structure.serialize;

import org.dewdrop.structure.api.Event;
import org.dewdrop.structure.events.ReadEventData;
import org.dewdrop.structure.events.WriteEventData;
import java.util.Map;
import java.util.Optional;

public interface EventSerializer {
    Optional<WriteEventData> serialize(Object event, Map<String, Object> headers);

    Optional<WriteEventData> serialize(Object event);

    <T extends Event> Optional<T> deserialize(ReadEventData event);
}

