package org.dewdrop.read.readmodel;

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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import org.dewdrop.fixture.events.DewdropUserCreated;
import org.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummaryReadModel;
import org.dewdrop.read.readmodel.cache.InMemoryCacheProcessor;
import org.dewdrop.read.readmodel.cache.MapBackedInMemoryCacheProcessor;
import org.dewdrop.read.readmodel.stream.Stream;
import org.dewdrop.structure.api.Event;
import org.dewdrop.utils.CacheUtils;
import org.dewdrop.utils.ReadModelUtils;
import org.hamcrest.MatcherAssert;
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
        Optional<ReadModelWrapper> readModelWrapper = ReadModelWrapper.of(DewdropAccountSummaryReadModel.class);
        readModel = spy(new ReadModel<>(readModelWrapper.get(), Optional.of(inMemoryCacheProcessor)));
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
        DewdropFundsAddedToAccount event = new DewdropFundsAddedToAccount(UUID.randomUUID(), new BigDecimal(10));
        doNothing().when(inMemoryCacheProcessor).process(any(Event.class));

        ReadModelWrapper readModelWrapper = mock(ReadModelWrapper.class);
        doNothing().when(readModelWrapper).callEventHandlers(any(Event.class));
        readModel.setReadModelWrapper(readModelWrapper);
        try (MockedStatic<CacheUtils> utilities = mockStatic(CacheUtils.class)) {
            utilities.when(() -> CacheUtils.getCacheRootKey(mock(Event.class))).thenReturn(Optional.of(UUID.randomUUID()));
            readModel.process(event);
            verify(inMemoryCacheProcessor, times(1)).process(any(Event.class));
            verify(readModelWrapper, times(1)).callEventHandlers(any(Event.class));
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
        assertThat(readModel.getReadModelWrapper().getOriginalReadModelClass(), is(targetReadModel.getClass()));
        assertThat(readModel.getReadModelWrapper().getReadModel().getClass(), is(targetReadModel.getClass()));
    }

    @Test
    @DisplayName("addStream() - Given a valid stream, should add the stream to the list of streams")
    void addStream() {
        Stream<Event> stream = mock(Stream.class);
        readModel.addStream(stream);
        MatcherAssert.assertThat(readModel.getStreams(), contains(stream));
    }

    @Test
    @DisplayName("updateState() - Given a readModel, should call inMemoryCacheProcessor.updateState()")
    void updateState() {
        Stream<Event> stream = mock(Stream.class);
        readModel.addStream(stream);
        doReturn(new HashMap<>()).when(inMemoryCacheProcessor).getCache();

        try (MockedStatic<ReadModelUtils> utilities = mockStatic(ReadModelUtils.class)) {
            readModel.updateState();
            utilities.verify(() -> ReadModelUtils.updateReadModelCacheField(any(Field.class), any(Object.class), any()), times(1));
            verify(stream, times(1)).updateState();
        }
    }
}
