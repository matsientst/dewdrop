package com.dewdrop.read.readmodel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummary;
import com.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummaryReadModel;
import com.dewdrop.read.readmodel.annotation.Stream;
import com.dewdrop.read.readmodel.stream.StreamDetails;
import com.dewdrop.read.readmodel.stream.StreamFactory;
import com.dewdrop.structure.api.Event;
import com.dewdrop.structure.datastore.StreamStore;
import com.dewdrop.structure.serialize.EventSerializer;
import com.dewdrop.utils.DependencyInjectionUtils;
import com.dewdrop.utils.EventHandlerUtils;
import com.dewdrop.utils.ReadModelUtils;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class ReadModelFactoryTest {
    StreamStore streamStore;
    EventSerializer eventSerializer;
    StreamFactory streamFactory;

    ReadModelFactory readModelFactory;
    ReadModel<Event> readModel;

    @BeforeEach
    void setup() {
        streamStore = mock(StreamStore.class);
        eventSerializer = mock(EventSerializer.class);
        streamFactory = mock(StreamFactory.class);

        readModelFactory = spy(new ReadModelFactory(streamStore, eventSerializer, streamFactory));
        readModel = spy(new ReadModel<>(new DewdropAccountSummaryReadModel(), null));
    }

    @Test
    @DisplayName("constructor() - should have a valid readModelFactory")
    void constructor() {
        assertThat(readModelFactory, is(notNullValue()));
    }

    @Test
    @DisplayName("constructReadModel() - Given a readModel target with a @ReadModel annotation, when I construct a readModel, then a ReadModelConstructed should be returned and subscribe() should have been called")
    void constructReadModel() {
        ReadModel<Event> readModel = spy(new ReadModel<>(new DewdropAccountSummaryReadModel(), null));
        doReturn(readModel).when(readModelFactory)
            .construct(any());
        doNothing().when(readModel)
            .subscribe();

        try (MockedStatic<DependencyInjectionUtils> utilities = mockStatic(DependencyInjectionUtils.class)) {
            utilities.when(() -> DependencyInjectionUtils.getInstance(any(Class.class)))
                .thenReturn(Optional.of(mock(DewdropAccountSummaryReadModel.class)));

            Optional<ReadModelConstructed> readModelConstructed = readModelFactory.constructReadModel(DewdropAccountSummaryReadModel.class);
            assertThat(readModelConstructed.isPresent(), is(true));
            verify(readModel, times(1)).subscribe();
        }
    }

    @Test
    @DisplayName("constructReadModel() - Given a readModel target with a @ReadModel annotation, when I fail to construct a readModel, then an empty Optional should be returned")
    void constructReadModel_cannotConstructReadModel() {

        doReturn(null).when(readModelFactory)
            .construct(any());
        doNothing().when(readModel)
            .subscribe();

        try (MockedStatic<DependencyInjectionUtils> utilities = mockStatic(DependencyInjectionUtils.class)) {
            utilities.when(() -> DependencyInjectionUtils.getInstance(any(Class.class)))
                .thenReturn(Optional.of(mock(DewdropAccountSummaryReadModel.class)));

            Optional<ReadModelConstructed> readModelConstructed = readModelFactory.constructReadModel(DewdropAccountSummaryReadModel.class);
            assertThat(readModelConstructed.isEmpty(), is(true));
            verify(readModel, times(0)).subscribe();
        }
    }

    @Test
    @DisplayName("constructReadModel() - Given a readModel target with a @ReadModel annotation, when I am unable to construct a readModel, then an empty Optional should be returned")
    void constructReadModel_invalidTarget() {
        try (MockedStatic<DependencyInjectionUtils> utilities = mockStatic(DependencyInjectionUtils.class)) {
            utilities.when(() -> DependencyInjectionUtils.getInstance(any(Class.class)))
                .thenReturn(Optional.empty());

            Optional<ReadModelConstructed> readModelConstructed = readModelFactory.constructReadModel(DewdropAccountSummaryReadModel.class);
            assertThat(readModelConstructed.isPresent(), is(false));
        }
    }

    @Test
    @DisplayName("construct() - Given a readModel target with a @ReadModel annotation, when I call construct(), than a ReadModel should be returned")
    void construct() {
        com.dewdrop.read.readmodel.stream.Stream stream = mock(com.dewdrop.read.readmodel.stream.Stream.class);
        StreamDetails streamDetails = mock(StreamDetails.class);
        doReturn("test").when(streamDetails).getStreamName();
        doReturn(true).when(streamDetails).isSubscribed();
        doReturn(streamDetails).when(stream).getStreamDetails();
        doReturn(stream).when(streamFactory)
            .constructStream(any(Stream.class), any(Consumer.class), anyList());
        doNothing().when(readModel)
            .addStream(any(com.dewdrop.read.readmodel.stream.Stream.class));
        try (MockedStatic<ReadModelUtils> utilities = mockStatic(ReadModelUtils.class)) {
            utilities.when(() -> ReadModelUtils.createReadModel(any()))
                .thenReturn(readModel);
            try (MockedStatic<EventHandlerUtils> eventUtilities = mockStatic(EventHandlerUtils.class)) {
                utilities.when(() -> ReadModelUtils.createReadModel(any(Class.class)))
                    .thenReturn(readModel);
                eventUtilities.when(() -> EventHandlerUtils.getEventHandlers(any(ReadModel.class)))
                    .thenReturn(List.of(DewdropAccountSummary.class));

                ReadModel<Event> result = readModelFactory.construct(new DewdropAccountSummaryReadModel());
                assertThat(result, is(notNullValue()));
                verify(readModel, times(2)).addStream(any(com.dewdrop.read.readmodel.stream.Stream.class));
            }
        }
    }
}
