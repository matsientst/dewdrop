package com.dewdrop.read.readmodel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dewdrop.fixture.events.DewdropUserCreated;
import com.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummaryReadModel;
import com.dewdrop.read.readmodel.cache.InMemoryCacheProcessor;
import com.dewdrop.read.readmodel.cache.MapBackedInMemoryCacheProcessor;
import com.dewdrop.read.readmodel.stream.Stream;
import com.dewdrop.structure.api.Event;
import com.dewdrop.utils.CacheUtils;
import com.dewdrop.utils.EventHandlerUtils;
import com.dewdrop.utils.ReadModelUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class ReadModelTest {
    ReadModel<Event> readModel;
    InMemoryCacheProcessor inMemoryCacheProcessor;
    DewdropAccountSummaryReadModel targetReadModel;

    @BeforeEach
    void setup() {
        inMemoryCacheProcessor = mock(MapBackedInMemoryCacheProcessor.class);
        targetReadModel = new DewdropAccountSummaryReadModel();
        readModel = spy(new ReadModel<>(targetReadModel, Optional.of(inMemoryCacheProcessor)));
    }

    @Test
    @DisplayName("constructor - should create a read model")
    void constructor() {
        assertThat(readModel, is(notNullValue()));
    }

    @Test
    @DisplayName("subscribe() - Given two stream subscriptions, should call subscribe on both")
    void subscribe() {
        Stream<Event> stream = mock(Stream.class);
        readModel.addStream(stream);
        readModel.addStream(stream);

        readModel.subscribe();
        verify(stream, times(2)).subscribe();
    }

    @Test
    @DisplayName("process() - Given an event, should call inMemoryCacheProcessor.process(), EventHandlerUtils.callEventHandler() and EventHandlerUtils.callOnEvent()")
    void process() {
        doNothing().when(inMemoryCacheProcessor).process(any(Event.class));
        try (MockedStatic<CacheUtils> utilities = mockStatic(CacheUtils.class)) {
            utilities.when(() -> CacheUtils.getCacheRootKey(mock(Event.class))).thenReturn(Optional.of(UUID.randomUUID()));
            try (MockedStatic<EventHandlerUtils> eventHandlerUtils = mockStatic(EventHandlerUtils.class)) {
                readModel.process(mock(Event.class));
                verify(inMemoryCacheProcessor, times(1)).process(any(Event.class));
                eventHandlerUtils.verify(() -> EventHandlerUtils.callEventHandler(any(Object.class), any(Event.class)), times(1));
                eventHandlerUtils.verify(() -> EventHandlerUtils.callOnEvent(any(Object.class), any(Event.class)), times(1));
            }
        }
    }

    @Test
    @DisplayName("handler() - Given an event, when handler is called, should call readModel.process()")
    void handler() {
        doNothing().when(readModel).process(any(Event.class));
        assertThat(readModel.handler(), is(notNullValue()));
    }

    @Test
    @DisplayName("handle() - Given an event, when handler is called, should call readModel.process()")
    void handle() {
        doNothing().when(readModel).process(any(Event.class));
        readModel.handle(new DewdropUserCreated(UUID.randomUUID(), "Test"));
        verify(readModel, times(1)).process(any(Event.class));
    }


    @Test
    @DisplayName("getCache() - Given a valid inMemoryCacheProcessor, should return the cache")
    void getCachedItems() {
        Map<UUID, Object> cache = new HashMap<>();
        doReturn(cache).when(inMemoryCacheProcessor).getCache();
        Object cachedItems = readModel.getCachedItems();
        assertThat(cachedItems, is(cache));
    }

    @Test
    @DisplayName("getCache() - Given a null inMemoryCacheProcessor, should return null")
    void getCachedItems_null() {
        readModel.setInMemoryCacheProcessor(Optional.empty());
        Object cachedItems = readModel.getCachedItems();
        assertThat(cachedItems, is(nullValue()));
    }

    @Test
    @DisplayName("getReadModel() - Given a valid targetReadModel, should return the targetReadModel")
    void getReadModel() {
        assertThat(readModel.getReadModel(), is(targetReadModel));
    }

    @Test
    @DisplayName("addStream() - Given a valid stream, should add the stream to the list of streams")
    void addStream() {
        Stream<Event> stream = mock(Stream.class);
        readModel.addStream(stream);
        assertThat(readModel.getStreams(), contains(stream));
    }

    @Test
    @DisplayName("updateState() - Given a readModel, should call inMemoryCacheProcessor.updateState()")
    void updateState() {
        Stream<Event> stream = mock(Stream.class);
        readModel.addStream(stream);
        doReturn(new HashMap<>()).when(inMemoryCacheProcessor).getCache();
        try (MockedStatic<ReadModelUtils> utilities = mockStatic(ReadModelUtils.class)) {
            readModel.updateState();
            utilities.verify(() -> ReadModelUtils.updateReadModelCacheField(any(Object.class), any()), times(1));
            verify(stream, times(1)).updateState();
        }
    }
}
