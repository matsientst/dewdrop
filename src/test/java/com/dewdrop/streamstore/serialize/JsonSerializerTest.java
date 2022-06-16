package com.dewdrop.streamstore.serialize;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.dewdrop.fixture.events.DewdropAccountCreated;
import com.dewdrop.structure.api.Message;
import com.dewdrop.structure.events.ReadEventData;
import com.dewdrop.structure.events.WriteEventData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.collections4.map.HashedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class JsonSerializerTest {
    ObjectMapper objectMapper;
    JsonSerializer jsonSerializer;
    Map<String, Object> commitHeaders;
    Message message;
    ReadEventData readEventData;

    @BeforeEach
    void setup() throws JsonProcessingException {
        objectMapper = spy(new ObjectMapper());
        jsonSerializer = spy(new JsonSerializer(objectMapper));
        DewdropAccountCreated test = new DewdropAccountCreated(UUID.randomUUID(), "test", UUID.randomUUID());
        message = spy(test);

        readEventData = new ReadEventData(UUID.randomUUID().toString(), UUID.randomUUID(), 3L, "TestEvent", objectMapper.writeValueAsBytes(message), objectMapper.writeValueAsBytes(commitHeaders), true, Instant.now());

        commitHeaders = new HashMap<>();
        commitHeaders.put("CommitId", UUID.randomUUID());
        commitHeaders.put("AggregateClrTypeName", message.getClass().getName());
        commitHeaders.put(JsonSerializer.EVENT_CLASS, message.getClass().getName());
    }

    @Test
    void serialize() {
        WriteEventData eventData = jsonSerializer.serialize(message, commitHeaders).orElse(null);

        assertEquals(DewdropAccountCreated.class.getSimpleName(), eventData.getEventType());
    }

    @Test
    void serialize_OneParameter() throws IOException {
        WriteEventData eventData = jsonSerializer.serialize(message).orElse(null);

        assertWriteEventData(eventData);
    }


    @Test
    void serialize_NullHeaders() throws IOException {
        WriteEventData eventData = jsonSerializer.serialize(message, null).orElse(null);

        assertWriteEventData(eventData);
    }

    @Test
    void serialize_mappingException() throws JsonProcessingException {
        doThrow(JsonProcessingException.class).when(objectMapper).writeValueAsBytes(anyMap());

        assertThat(jsonSerializer.serialize(message).isEmpty(), is(true));
    }


    @Test
    void deserialize() throws JsonProcessingException {
        ReadEventData eventData = new ReadEventData(UUID.randomUUID().toString(), UUID.randomUUID(), 3L, "TestEvent", objectMapper.writeValueAsBytes(message), objectMapper.writeValueAsBytes(commitHeaders), true, Instant.now());

        Optional<DewdropAccountCreated> deserialize = jsonSerializer.deserialize(eventData);
        DewdropAccountCreated result = deserialize.orElse(null);

        DewdropAccountCreated castedEvent = (DewdropAccountCreated) message;

        assertThat(result.getAccountId(), is(castedEvent.getAccountId()));
        assertThat(result.getName(), is(castedEvent.getName()));
    }

    @Test
    void deserialize_exception() throws IOException {
        doThrow(IOException.class).when(objectMapper).readValue(any(byte[].class), any(Class.class));
        Optional<DewdropAccountCreated> result = jsonSerializer.deserialize(readEventData);
        assertThat(result.isEmpty(), is(true));

        doReturn(null).when(objectMapper).writeValueAsBytes(anyMap());
        Optional<WriteEventData> eventData = new JsonSerializer(objectMapper).serialize(message, new HashMap<>());
        Optional<DewdropAccountCreated> response = jsonSerializer.deserialize(readEventData);
        assertThat(response.isEmpty(), is(true));
    }

    @Test
    void deserialize_exception_metadataHasBytes() throws IOException {
        doThrow(IOException.class).when(objectMapper).readValue(any(byte[].class), any(Class.class));
        ReadEventData readEventData = mock(ReadEventData.class);
        doReturn("test".getBytes()).when(readEventData).getMetadata();

        Optional<DewdropAccountCreated> result = jsonSerializer.deserialize(readEventData);
        assertThat(result.isEmpty(), is(true));

        doThrow(IOException.class).when(objectMapper).readValue(any(byte[].class), any(Class.class));
        doReturn("test".getBytes()).doReturn(null).when(readEventData).getMetadata();
        Optional<DewdropAccountCreated> response = jsonSerializer.deserialize(readEventData);
        assertThat(response.isEmpty(), is(true));
    }

    @Test
    void deserializeEvent() throws IOException {
        doThrow(IOException.class).when(objectMapper).readValue(any(byte[].class), any(Class.class));
        Object result = jsonSerializer.deserializeEvent(readEventData, message.getClass().getName(), new HashedMap<>());
        assertThat(result, is(Optional.empty()));
    }

    @Test
    void deserializeEvent_eventAndClassName_ClassNotFound() throws IOException {
        Object result = jsonSerializer.deserializeEvent(readEventData, "Test", new HashedMap<>());
        assertThat(result, is(Optional.empty()));
    }

    private void assertWriteEventData(WriteEventData eventData) throws IOException {
        assertThat(DewdropAccountCreated.class.getSimpleName(), is(eventData.getEventType()));
        assertThat(eventData.getEventId(), is(notNullValue()));
        assertThat(eventData.isJson(), is(true));
        assertThat(eventData.isJson(), is(true));
        assertThat(eventData.getMetadata().length, is(greaterThan(0)));
        assertThat(eventData.getData().length, is(greaterThan(0)));
        DewdropAccountCreated testEvent = objectMapper.readValue(eventData.getData(), DewdropAccountCreated.class);
        assertThat(testEvent.getName(), is("test"));
        assertThat(testEvent.getAccountId(), is(((DewdropAccountCreated) message).getAccountId()));
    }
}
