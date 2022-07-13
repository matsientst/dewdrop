package events.dewdrop.read.readmodel;

import static org.hamcrest.MatcherAssert.assertThat;
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

import events.dewdrop.fixture.readmodel.AccountCreatedService;
import events.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummary;
import events.dewdrop.read.readmodel.stream.Stream;
import events.dewdrop.read.readmodel.stream.StreamDetails;
import events.dewdrop.read.readmodel.stream.StreamFactory;
import events.dewdrop.utils.DependencyInjectionUtils;
import events.dewdrop.utils.EventHandlerUtils;
import events.dewdrop.utils.ReadModelUtils;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import events.dewdrop.fixture.events.DewdropAccountCreated;
import events.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummaryReadModel;
import events.dewdrop.structure.api.Event;
import events.dewdrop.structure.datastore.StreamStore;
import events.dewdrop.structure.serialize.EventSerializer;
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
        readModel = spy(new ReadModel<>(ReadModelWrapper.of(DewdropAccountSummaryReadModel.class).get(), null));
    }

    @Test
    @DisplayName("constructor() - should have a valid readModelFactory")
    void constructor() {
        assertThat(readModelFactory, is(notNullValue()));
    }

    @Test
    @DisplayName("constructReadModel() - Given a readModel target with a @ReadModel annotation, when I construct a readModel, then a ReadModelConstructed should be returned and subscribe() should have been called")
    void constructReadModel() {
        ReadModel<Event> readModel = spy(new ReadModel<>(ReadModelWrapper.of(DewdropAccountSummaryReadModel.class).get(), null));
        doReturn(readModel).when(readModelFactory).construct(any());
        doNothing().when(readModel).subscribe();

        try (MockedStatic<DependencyInjectionUtils> utilities = mockStatic(DependencyInjectionUtils.class)) {
            utilities.when(() -> DependencyInjectionUtils.getInstance(any(Class.class))).thenReturn(Optional.of(mock(DewdropAccountSummaryReadModel.class)));

            Optional<ReadModelConstructed> readModelConstructed = readModelFactory.constructReadModel(DewdropAccountSummaryReadModel.class);
            assertThat(readModelConstructed.isPresent(), is(true));
            verify(readModel, times(1)).subscribe();
        }
    }

    @Test
    @DisplayName("constructReadModel() - Given a readModel target with a @ReadModel annotation, when I fail to construct a readModel, then an empty Optional should be returned")
    void constructReadModel_cannotConstructReadModel() {

        doReturn(null).when(readModelFactory).construct(any());
        doNothing().when(readModel).subscribe();

        try (MockedStatic<DependencyInjectionUtils> utilities = mockStatic(DependencyInjectionUtils.class)) {
            utilities.when(() -> DependencyInjectionUtils.getInstance(any(Class.class))).thenReturn(Optional.of(mock(DewdropAccountSummaryReadModel.class)));

            Optional<ReadModelConstructed> readModelConstructed = readModelFactory.constructReadModel(DewdropAccountSummaryReadModel.class);
            assertThat(readModelConstructed.isEmpty(), is(true));
            verify(readModel, times(0)).subscribe();
        }
    }

    @Test
    @DisplayName("constructReadModel() - Given a readModel target with a @ReadModel annotation, when I am unable to construct a readModel, then an empty Optional should be returned")
    void constructReadModel_invalidTarget() {
        try (MockedStatic<DependencyInjectionUtils> utilities = mockStatic(DependencyInjectionUtils.class)) {
            utilities.when(() -> DependencyInjectionUtils.getInstance(any(Class.class))).thenReturn(Optional.empty());

            Optional<ReadModelConstructed> readModelConstructed = readModelFactory.constructReadModel(DewdropAccountSummaryReadModel.class);
            assertThat(readModelConstructed.isPresent(), is(false));
        }
    }

    @Test
    @DisplayName("construct() - Given a readModel target with a @ReadModel annotation, when I call construct(), than a ReadModel should be returned")
    void construct() {
        Stream stream = mock(Stream.class);
        StreamDetails streamDetails = mock(StreamDetails.class);
        doReturn("test").when(streamDetails).getStreamName();
        doReturn(true).when(streamDetails).isSubscribed();
        doReturn(streamDetails).when(stream).getStreamDetails();
        doReturn(stream).when(streamFactory).constructStreamFromStream(any(events.dewdrop.read.readmodel.annotation.Stream.class), any(ReadModel.class));
        doNothing().when(readModel).addStream(any(Stream.class));
        try (MockedStatic<ReadModelUtils> utilities = mockStatic(ReadModelUtils.class)) {
            utilities.when(() -> ReadModelUtils.createReadModel(any())).thenReturn(readModel);
            try (MockedStatic<EventHandlerUtils> eventUtilities = mockStatic(EventHandlerUtils.class)) {
                utilities.when(() -> ReadModelUtils.createReadModel(any(ReadModelWrapper.class))).thenReturn(readModel);
                eventUtilities.when(() -> EventHandlerUtils.getEventHandlers(any(ReadModel.class))).thenReturn(List.of(DewdropAccountSummary.class));

                ReadModelWrapper readModelWrapper = ReadModelWrapper.of(DewdropAccountSummaryReadModel.class).get();
                ReadModel<Event> result = readModelFactory.construct(readModelWrapper);
                assertThat(result, is(notNullValue()));
                verify(readModel, times(2)).addStream(any(Stream.class));
            }
        }
    }

    @Test
    @DisplayName("construct() - Given a readModel target without an @Stream annotation, when I call construct(), than a null ReadModel should be returned")
    void construct_noStreamAnnotation() {
        try (MockedStatic<ReadModelUtils> utilities = mockStatic(ReadModelUtils.class)) {
            utilities.when(() -> ReadModelUtils.createReadModel(any())).thenReturn(readModel);

            ReadModel<Event> result = readModelFactory.construct(ReadModelWrapper.of(String.class).get());
            assertThat(result, is(nullValue()));
        }
    }

    @Test
    @DisplayName("createReadModelForOnEvent() - Given a method with a @OnEvent annotation, when I call createReadModelForOnEvent(), then a ReadModel should be returned")
    void createReadModelForOnEvent() {
        Method method = mock(Method.class);
        doReturn(AccountCreatedService.class).when(method).getDeclaringClass();
        doReturn(new Class[] {DewdropAccountCreated.class}).when(method).getParameterTypes();
        Stream stream = mock(Stream.class);

        doReturn(stream).when(streamFactory).constructStreamForEvent(any(Consumer.class), any(Class.class));
        try (MockedStatic<DependencyInjectionUtils> utilities = mockStatic(DependencyInjectionUtils.class)) {
            utilities.when(() -> DependencyInjectionUtils.getInstance(any(Class.class))).thenReturn(Optional.of(mock(AccountCreatedService.class)));

            ReadModel<Event> result = readModelFactory.createReadModelForOnEvent(method);
            assertThat(result, is(notNullValue()));
            verify(stream, times(1)).subscribe();
        }
    }

    @Test
    @DisplayName("createReadModelForOnEvent() - Given a method with a @OnEvent annotation, when I have no instance of my object, then a null ReadModel should be returned")
    void createReadModelForOnEvent_noInstance() {
        Method method = mock(Method.class);
        doReturn(AccountCreatedService.class).when(method).getDeclaringClass();
        try (MockedStatic<DependencyInjectionUtils> utilities = mockStatic(DependencyInjectionUtils.class)) {
            utilities.when(() -> DependencyInjectionUtils.getInstance(any(Class.class))).thenReturn(Optional.empty());

            ReadModel<Event> result = readModelFactory.createReadModelForOnEvent(method);
            assertThat(result, is(nullValue()));
        }
    }

    @Test
    @DisplayName("createReadModelForOnEvent() - Given a method with a @OnEvent annotation, when I my first parameter does not extend Event, then a null ReadModel should be returned")
    void createReadModelForOnEvent_firstParameterIsNotAnEvent() {
        Method method = mock(Method.class);
        doReturn(AccountCreatedService.class).when(method).getDeclaringClass();
        doReturn(new Class[] {String.class}).when(method).getParameterTypes();
        try (MockedStatic<DependencyInjectionUtils> utilities = mockStatic(DependencyInjectionUtils.class)) {
            utilities.when(() -> DependencyInjectionUtils.getInstance(any(Class.class))).thenReturn(Optional.of(mock(AccountCreatedService.class)));

            ReadModel<Event> result = readModelFactory.createReadModelForOnEvent(method);
            assertThat(result, is(nullValue()));
        }
    }

    @Test
    @DisplayName("createReadModelForOnEvent() - Given a method with a @OnEvent annotation, when there are no parameters for the OnEvent method, then a null ReadModel should be returned")
    void createReadModelForOnEvent_noParameters() {
        Method method = mock(Method.class);
        doReturn(AccountCreatedService.class).when(method).getDeclaringClass();
        doReturn(new Class[] {}).when(method).getParameterTypes();
        try (MockedStatic<DependencyInjectionUtils> utilities = mockStatic(DependencyInjectionUtils.class)) {
            utilities.when(() -> DependencyInjectionUtils.getInstance(any(Class.class))).thenReturn(Optional.of(mock(AccountCreatedService.class)));

            ReadModel<Event> result = readModelFactory.createReadModelForOnEvent(method);
            assertThat(result, is(nullValue()));
        }
    }
}
