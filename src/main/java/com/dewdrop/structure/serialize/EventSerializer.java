package com.dewdrop.structure.serialize;

import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.events.ReadEventData;
import com.dewdrop.structure.events.WriteEventData;
import java.util.Map;
import java.util.Optional;

public interface EventSerializer {
    Optional<WriteEventData> serialize(Object event, Map<String, Object> headers);

    Optional<WriteEventData> serialize(Object event);

    <T extends Event> Optional<T> deserialize(ReadEventData event);
}

