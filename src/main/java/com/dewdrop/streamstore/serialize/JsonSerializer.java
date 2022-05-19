package com.dewdrop.streamstore.serialize;

import static com.dewdrop.streamstore.repository.StreamStoreRepository.CAUSATION_ID;
import static com.dewdrop.streamstore.repository.StreamStoreRepository.CORRELATION_ID;

import com.dewdrop.structure.events.CorrelationCausation;
import com.dewdrop.structure.events.ReadEventData;
import com.dewdrop.structure.events.WriteEventData;
import com.dewdrop.structure.serialize.EventSerializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

@Log4j2
public class JsonSerializer implements EventSerializer {
    private ObjectMapper objectMapper;
    public static final String EVENT_CLASS = "EventFullClassName";

    public JsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<WriteEventData> serialize(Object event) {
        return serialize(event, null);
    }

    @Override
    public Optional<WriteEventData> serialize(Object event, Map<String, Object> headers) {
        headers = Optional.ofNullable(headers).orElse(new HashMap<>());

        headers.computeIfAbsent(EVENT_CLASS, name -> event.getClass().getName());

        String typeName = event.getClass().getSimpleName();

        try {
            byte[] metadata = objectMapper.writeValueAsBytes(headers);
            byte[] data = objectMapper.writeValueAsBytes(event);
            WriteEventData writeEventData = new WriteEventData(UUID.randomUUID(), typeName, true, data, metadata);
            return Optional.of(writeEventData);
        } catch (JsonProcessingException e) {
            log.error("problem serializing json for type:" + typeName, e);
            return Optional.empty();
        }
    }

    @Override
    public <T> Optional<T> deserialize(ReadEventData event) {
        Map<String, Object> metadata = new HashMap<>();
        try {
            metadata = objectMapper.readValue(event.getMetadata(), Map.class);
        } catch (IOException e) {
            Integer length = event.getMetadata() == null ? 0 : event.getMetadata().length;

            log.error("problem deserialize metadata for event {} - size of metaData:{}", event.getEventType(), length, e);
        }
        String className = (String) metadata.get(EVENT_CLASS);
        if (StringUtils.isBlank(className)) {
            log.error("className not found for eventType:{}", event.getEventType());
            return Optional.empty();
        }

        return deserializeEvent(event, className, metadata);
    }

    public <T> Optional<T> deserializeEvent(ReadEventData event, String className, Map<String, Object> metadata) {
        try {
            T value = (T) objectMapper.readValue(event.getData(), Class.forName(className));
            if(value instanceof CorrelationCausation) {
                CorrelationCausation correlationCausation = (CorrelationCausation) value;
                if(metadata.containsKey(CAUSATION_ID)) {
                    String uuid = (String) metadata.get(CAUSATION_ID);
                    correlationCausation.setCausationId(UUID.fromString(uuid));
                }
                if(metadata.containsKey(CORRELATION_ID)) {
                    String uuid = (String) metadata.get(CORRELATION_ID);
                    correlationCausation.setCorrelationId(UUID.fromString(uuid));
                }
            }

            return Optional.of(value);
        } catch (IOException e) {
            log.error("Unable to deserialize data for class:" + className, e);
            return Optional.empty();
        } catch (ClassNotFoundException e) {
            log.error("Unable to deserialize data - class not found:" + className, e);
            return Optional.empty();
        }
    }
}
