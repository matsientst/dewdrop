package org.dewdrop.read.readmodel;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.jodah.expiringmap.ExpiringMap;
import org.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetailsReadModel;
import org.dewdrop.fixture.readmodel.accountdetails.details.DewdropGetAccountByIdQuery;
import org.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummaryQuery;
import org.dewdrop.fixture.readmodel.accountdetails.summary.DewdropAccountSummaryReadModel;
import org.dewdrop.structure.api.Event;
import org.dewdrop.utils.DewdropAnnotationUtils;
import org.dewdrop.utils.ReadModelUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class DefaultAnnotationReadModelMapperTest {
    DefaultAnnotationReadModelMapper readModelMapper;
    ReadModelFactory readModelFactory;
    Class<DewdropGetAccountByIdQuery> queryClass = DewdropGetAccountByIdQuery.class;
    ReadModel readModel = new ReadModel(ReadModelWrapper.of(DewdropAccountDetailsReadModel.class).get(), null);

    @BeforeEach
    void setup() {
        readModelMapper = spy(new DefaultAnnotationReadModelMapper());
        readModelFactory = mock(ReadModelFactory.class);
        readModelMapper.setReadModelFactory(readModelFactory);
        readModelMapper.EPHEMERAL_READ_MODELS = ExpiringMap.builder().maxSize(1).variableExpiration().build();
    }

    @Test
    @DisplayName("init() - Given a readModelMapper, when we call init(), confirm that we have a readModelFactory and that we call registerReadModels()")
    void init() {
        readModelMapper.setReadModelFactory(null);
        doNothing().when(readModelMapper).registerReadModels();
        readModelMapper.init(readModelFactory);

        assertThat(readModelMapper.getReadModelFactory(), is(notNullValue()));
        verify(readModelMapper, times(1)).registerReadModels();
    }

    @Test
    @DisplayName("registerReadModels() - Given two readModelClasses (one ephemeral and one not), when registerReadModels() is called, then confirm we have an EPHEMERAL_READ_MODELS, and that we called registerQueryHandlers() twice and registerOnEvents() once")
    void registerReadModels() {
        ReadModelConstructed readModelConstructed = mock(ReadModelConstructed.class);
        doNothing().when(readModelMapper).registerQueryHandlers(any(Class.class), any(Optional.class));
        doReturn(Optional.of(readModelConstructed)).when(readModelFactory).constructReadModel(any(Class.class));
        doNothing().when(readModelMapper).registerOnEvents();
        try (MockedStatic<ReadModelUtils> utilities = mockStatic(ReadModelUtils.class)) {
            utilities.when(() -> ReadModelUtils.getAnnotatedReadModels()).thenReturn(List.of(DewdropAccountDetailsReadModel.class, DewdropAccountSummaryReadModel.class));
            utilities.when(() -> ReadModelUtils.isEphemeral(any(Class.class))).thenReturn(true).thenReturn(false);

            readModelMapper.registerReadModels();
            assertThat(readModelMapper.EPHEMERAL_READ_MODELS, is(notNullValue()));
            verify(readModelMapper, times(2)).registerQueryHandlers(any(Class.class), any(Optional.class));
            verify(readModelMapper, times(1)).registerOnEvents();
        }
    }

    @Test
    @DisplayName("registerReadModels() - Given one readModelClasses that is not ephemeral, when registerReadModels() is called, then confirm EPHEMERAL_READ_MODELS doesn't exist, and that we called registerQueryHandlers() once and registerOnEvents() once")
    void registerReadModels_noEphemeral() {
        readModelMapper.EPHEMERAL_READ_MODELS = null;
        ReadModelConstructed readModelConstructed = mock(ReadModelConstructed.class);
        doNothing().when(readModelMapper).registerQueryHandlers(any(Class.class), any(Optional.class));
        doReturn(Optional.of(readModelConstructed)).when(readModelFactory).constructReadModel(any(Class.class));
        doNothing().when(readModelMapper).registerOnEvents();
        try (MockedStatic<ReadModelUtils> utilities = mockStatic(ReadModelUtils.class)) {
            utilities.when(() -> ReadModelUtils.getAnnotatedReadModels()).thenReturn(List.of(DewdropAccountSummaryReadModel.class));
            utilities.when(() -> ReadModelUtils.isEphemeral(any(Class.class))).thenReturn(false);

            readModelMapper.registerReadModels();
            assertThat(readModelMapper.EPHEMERAL_READ_MODELS, is(nullValue()));
            verify(readModelMapper, times(1)).registerQueryHandlers(any(Class.class), any(Optional.class));
            verify(readModelMapper, times(1)).registerOnEvents();
        }
    }

    @Test
    @DisplayName("registerQueryHandlers() - Given a readModelClass and an Optional.of(ReadModel), when registerQueryHandler() is called, then confirm QUERY_TO_READ_MODEL_CLASS has mapped the query class to the ReadModel and that we called addToQueryReadModelCache()")
    void registerQueryHandlers() {
        Method method = mock(Method.class);
        doReturn(new Class[] {queryClass}).when(method).getParameterTypes();
        ReadModelConstructed constructed = mock(ReadModelConstructed.class);
        doReturn(mock(ReadModel.class)).when(constructed).getReadModel();
        doNothing().when(readModelMapper).addToQueryReadModelCache(any(ReadModel.class), any(Method.class));

        try (MockedStatic<ReadModelUtils> utilities = mockStatic(ReadModelUtils.class)) {
            utilities.when(() -> ReadModelUtils.getQueryHandlerMethods(any(Class.class))).thenReturn(List.of(method));
            Class<DewdropAccountDetailsReadModel> readModelClass = DewdropAccountDetailsReadModel.class;
            readModelMapper.registerQueryHandlers(readModelClass, Optional.of(constructed));

            assertThat(readModelMapper.QUERY_TO_READ_MODEL_CLASS.get(queryClass), is(readModelClass));
            verify(readModelMapper, times(1)).addToQueryReadModelCache(any(ReadModel.class), any(Method.class));
        }
    }

    @Test
    @DisplayName("addToQueryReadModelCache() - Given a readModel and a queryMethodHandler, when addToQueryReadModelCache() is called, then confirm that QUERY_TO_READ_MODEL has the first parameter of the method as the key and the readModel as the value")
    void addToQueryReadModelCache() {
        Method method = mock(Method.class);

        doReturn(new Class[] {queryClass}).when(method).getParameterTypes();
        readModelMapper.addToQueryReadModelCache(readModel, method);

        assertThat(readModelMapper.QUERY_TO_READ_MODEL.get(queryClass), is(readModel));
    }

    @Test
    @DisplayName("registerOnEvents() - Given classes with methods that are annotated with @onEvent, When registerOnEvents is called, confirm that createReadModelForOnEvent() is called with the methods annotated")
    void registerOnEvents() {
        Method method = mock(Method.class);
        doReturn(mock(ReadModel.class)).when(readModelFactory).createReadModelForOnEvent(any(Method.class));
        try (MockedStatic<DewdropAnnotationUtils> utilities = mockStatic(DewdropAnnotationUtils.class)) {
            utilities.when(() -> DewdropAnnotationUtils.getAnnotatedMethods(any(Class.class))).thenReturn(Set.of(method));
            readModelMapper.registerOnEvents();

            verify(readModelFactory, times(1)).createReadModelForOnEvent(eq(method));
        }
    }

    @Test
    @DisplayName("getReadModelByQuery() - Given a query object, When we have an entry in QUERY_TO_READ_MODEL with the query class, then return the ReadModel")
    void getReadModelByQuery_queryToReadModel() {
        DewdropAccountSummaryQuery query = new DewdropAccountSummaryQuery();
        readModelMapper.QUERY_TO_READ_MODEL.put(query.getClass(), readModel);
        Optional<ReadModel<Event>> readModelByQuery = readModelMapper.getReadModelByQuery(query);
        assertThat(readModelByQuery.isPresent(), is(true));
    }

    @Test
    @DisplayName("getReadModelByQuery() - Given a query object, When we have a cached ephemeral ReadModel, then return the ReadModel")
    void getReadModelByQuery_ephemeral_cached() {
        DewdropAccountSummaryQuery query = new DewdropAccountSummaryQuery();
        readModelMapper.QUERY_TO_READ_MODEL = new HashMap<>();
        readModelMapper.QUERY_TO_READ_MODEL_CLASS.put(query.getClass(), readModel.getClass());
        doReturn(null).when(readModelMapper).createAndCacheEphemeralReadModel(any(Class.class));

        readModelMapper.EPHEMERAL_READ_MODELS.put(readModel.getClass(), readModel, 2, TimeUnit.SECONDS);
        Optional<ReadModel<Event>> readModelByQuery = readModelMapper.getReadModelByQuery(query);
        assertThat(readModelByQuery.isPresent(), is(true));

        with().pollInterval(fibonacci(SECONDS)).await().timeout(10L, SECONDS).until(() -> readModelMapper.getReadModelByQuery(query).isEmpty());
    }

    @Test
    @DisplayName("getReadModelByQuery() - Given a query object, When we have an ephemeral ReadModel, then call createAndCacheEphemeralReadModel() return the ReadModel")
    void getReadModelByQuery_ephemeral_notCached() {
        doReturn(readModel).when(readModelMapper).createAndCacheEphemeralReadModel(any(Class.class));
        DewdropAccountSummaryQuery query = new DewdropAccountSummaryQuery();
        readModelMapper.QUERY_TO_READ_MODEL = new HashMap<>();
        readModelMapper.QUERY_TO_READ_MODEL_CLASS.put(query.getClass(), readModel.getClass());

        Optional<ReadModel<Event>> readModelByQuery = readModelMapper.getReadModelByQuery(query);

        assertThat(readModelByQuery.isPresent(), is(true));
        verify(readModelMapper, times(1)).createAndCacheEphemeralReadModel(any(Class.class));
    }

    @Test
    @DisplayName("getReadModelByQuery() - Given a query object, When we don't have it in the QUERY_TO_READ_MODEL_CLASS, then return an empty Optional")
    void getReadModelByQuery_ephemeral_unknown() {
        readModelMapper.QUERY_TO_READ_MODEL_CLASS = new HashMap<>();
        readModelMapper.QUERY_TO_READ_MODEL = new HashMap<>();
        DewdropAccountSummaryQuery query = new DewdropAccountSummaryQuery();
        Optional<ReadModel<Event>> readModelByQuery = readModelMapper.getReadModelByQuery(query);

        assertThat(readModelByQuery.isPresent(), is(false));
        verify(readModelMapper, times(0)).createAndCacheEphemeralReadModel(any(Class.class));
    }

    @Test
    @DisplayName("createAndCacheEphemeralReadModel() - Given a readModel class, When we have an ephemeral ReadModel with destroyImmediately, then construct and return the ReadModel, but do not cache it")
    void createAndCacheEphemeralReadModel_destroyImmediately() {
        ReadModelConstructed constructed = new ReadModelConstructed(readModel);
        doReturn(Optional.of(constructed)).when(readModelFactory).constructReadModel(any(Class.class));

        ReadModel<Event> andCacheEphemeralReadModel = readModelMapper.createAndCacheEphemeralReadModel(readModel.getClass());
        assertThat(andCacheEphemeralReadModel, is(notNullValue()));
        assertThat(readModelMapper.EPHEMERAL_READ_MODELS.get(readModel.getClass()), is(nullValue()));
    }

    @Test
    @DisplayName("createAndCacheEphemeralReadModel() - Given a readModel class, When we have an ephemeral ReadModel with NEVER_DESTROY, then construct and return the ReadModel, and add it to the cache indefinitely")
    void createAndCacheEphemeralReadModel_keepIndefinitely() {
        ReadModelConstructed constructed = mock(ReadModelConstructed.class);
        doReturn(Optional.of(constructed)).when(readModelFactory).constructReadModel(any(Class.class));
        doReturn(readModel).when(constructed).getReadModel();
        doReturn(-1).when(constructed).getDestroyInMinutesUnused();

        ReadModel<Event> andCacheEphemeralReadModel = readModelMapper.createAndCacheEphemeralReadModel(NeverDestroyReadModel.class);
        assertThat(andCacheEphemeralReadModel, is(notNullValue()));
        assertThat(readModelMapper.EPHEMERAL_READ_MODELS.get(NeverDestroyReadModel.class), is(notNullValue()));
        assertThat(readModelMapper.EPHEMERAL_READ_MODELS.getExpiration(NeverDestroyReadModel.class), is(greaterThan(Long.valueOf(Integer.MAX_VALUE - 1000))));
    }

    @Test
    @DisplayName("createAndCacheEphemeralReadModel() - Given a readModel class, When we have an ephemeral ReadModel with a destroyInMinutes of 60, then construct and return the ReadModel, and add it to the cache for 60 minutes")
    void createAndCacheEphemeralReadModel_keepForAnHour() {
        ReadModelConstructed constructed = mock(ReadModelConstructed.class);
        doReturn(Optional.of(constructed)).when(readModelFactory).constructReadModel(any(Class.class));
        doReturn(readModel).when(constructed).getReadModel();
        doReturn(60).when(constructed).getDestroyInMinutesUnused();


        ReadModel<Event> andCacheEphemeralReadModel = readModelMapper.createAndCacheEphemeralReadModel(KeepForAnHourReadModel.class);
        assertThat(andCacheEphemeralReadModel, is(notNullValue()));
        assertThat(readModelMapper.EPHEMERAL_READ_MODELS.get(KeepForAnHourReadModel.class), is(notNullValue()));
        assertThat(readModelMapper.EPHEMERAL_READ_MODELS.getExpiration(KeepForAnHourReadModel.class), is(3600000L));
    }

    @Test
    @DisplayName("createAndCacheEphemeralReadModel() - Given a readModel class, When we are unable to construct it, then return null")
    void createAndCacheEphemeralReadModel_unableToCreate() {
        doReturn(Optional.empty()).when(readModelFactory).constructReadModel(any(Class.class));

        ReadModel<Event> andCacheEphemeralReadModel = readModelMapper.createAndCacheEphemeralReadModel(KeepForAnHourReadModel.class);
        assertThat(andCacheEphemeralReadModel, is(nullValue()));
        assertThat(readModelMapper.EPHEMERAL_READ_MODELS.containsKey(KeepForAnHourReadModel.class), is(false));
    }

    @org.dewdrop.read.readmodel.annotation.ReadModel(ephemeral = true, destroyInMinutesUnused = org.dewdrop.read.readmodel.annotation.ReadModel.NEVER_DESTROY)
    private class NeverDestroyReadModel {
        public NeverDestroyReadModel() {}
    }

    @org.dewdrop.read.readmodel.annotation.ReadModel(ephemeral = true, destroyInMinutesUnused = 60)
    private class KeepForAnHourReadModel {
    }
}
