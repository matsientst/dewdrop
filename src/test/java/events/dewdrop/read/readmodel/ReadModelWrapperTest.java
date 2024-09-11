package events.dewdrop.read.readmodel;

import events.dewdrop.fixture.events.DewdropAccountCreated;
import events.dewdrop.fixture.events.DewdropFundsAddedToAccount;
import events.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetails;
import events.dewdrop.fixture.readmodel.accountdetails.details.DewdropAccountDetailsReadModel;
import events.dewdrop.read.readmodel.annotation.AggregateStream;
import events.dewdrop.read.readmodel.stream.StreamAnnotationDetails;
import events.dewdrop.utils.DependencyInjectionUtils;
import events.dewdrop.utils.DewdropReflectionUtils;
import events.dewdrop.utils.ReadModelUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

class ReadModelWrapperTest {
    @Test
    @DisplayName("of() - Given a class annotated with @ReadModel, when the class is instantiated, then the ReadModelWrapper is created")
    void of() {
        Optional<ReadModelWrapper> readModelWrapper = ReadModelWrapper.of(DewdropAccountDetailsReadModel.class);
        assertThat(readModelWrapper.isPresent(), is(true));
        ReadModelWrapper result = readModelWrapper.get();
        assertThat(result.getOriginalReadModelClass(), is(DewdropAccountDetailsReadModel.class));
        assertThat(result.getReadModel().getClass(), is(DewdropAccountDetailsReadModel.class));
        assertThat(result.getCacheField().isPresent(), is(true));
        Field cacheField = result.getCacheField().get();
        assertThat(cacheField.getType(), is(Map.class));
        assertThat(result.getSupportedEvents(), contains(DewdropAccountCreated.class));
    }

    @Test
    @DisplayName("of_dependencyInjected() - Given a class annotated with @ReadModel, when we retrieve an object from DI, then the ReadModelWrapper is created")
    void of_dependencyInjected() {
        try (MockedStatic<DependencyInjectionUtils> utilities = mockStatic(DependencyInjectionUtils.class)) {
            utilities.when(() -> DependencyInjectionUtils.getInstance(any(Class.class))).thenReturn(Optional.of(new SameAsDewdropAccountDetailsReadModel()));

            Optional<ReadModelWrapper> readModelWrapper = ReadModelWrapper.of(DewdropAccountDetailsReadModel.class);
            assertThat(readModelWrapper.isPresent(), is(true));
            ReadModelWrapper result = readModelWrapper.get();
            assertThat(result.getOriginalReadModelClass(), is(DewdropAccountDetailsReadModel.class));
            assertThat(result.getReadModel().getClass(), is(SameAsDewdropAccountDetailsReadModel.class));
            assertThat(result.getCacheField().isPresent(), is(true));
            Field cacheField = result.getCacheField().get();
            assertThat(cacheField.getType(), is(Map.class));
            assertThat(result.getSupportedEvents(), contains(DewdropAccountCreated.class));
        }
    }

    @Test
    @DisplayName("getStreamAnnotations() - Given a class annotated with @ReadModel, when we retrieve the @Stream annotations, then the stream annotations are returned")
    void getStreamAnnotations() {
        ReadModelWrapper readModelWrapper = ReadModelWrapper.of(DewdropAccountDetailsReadModel.class).get();
        List<StreamAnnotationDetails> streamAnnotations = readModelWrapper.getStreamAnnotations();
        assertThat(streamAnnotations.stream().filter(strm -> StringUtils.equalsAnyIgnoreCase(strm.getStreamName(), "DewdropAccountAggregate")).findAny().isPresent(), is(true));
        assertThat(streamAnnotations.stream().filter(strm -> StringUtils.equalsAnyIgnoreCase(strm.getStreamName(), "DewdropUserAggregate")).findAny().isPresent(), is(true));
    }

    @Test
    @DisplayName("callEventHandlers() - Given an event that is the first parameter of @EventHandler, when we call callEventHandlers(), then confirm we called DewdropReflectionUtils.callMethod()")
    void callEventHandlers() {
        ReadModelWrapper readModelWrapper = ReadModelWrapper.of(DewdropAccountDetailsReadModel.class).get();
        try (MockedStatic<DewdropReflectionUtils> utilities = mockStatic(DewdropReflectionUtils.class)) {
            readModelWrapper.callEventHandlers(new DewdropAccountCreated(UUID.randomUUID(), "test", UUID.randomUUID()));
            utilities.verify(() -> DewdropReflectionUtils.callMethod(any(), any(Method.class), any(DewdropAccountCreated.class)), times(1));
        }
    }

    @Test
    @DisplayName("callEventHandlers() - Given an event that is the first parameter of @EventHandler, when we call callEventHandlers(), then confirm we called DewdropReflectionUtils.callMethod()")
    void callEventHandlers_eventNotSupported() {
        ReadModelWrapper readModelWrapper = ReadModelWrapper.of(DewdropAccountDetailsReadModel.class).get();
        try (MockedStatic<DewdropReflectionUtils> utilities = mockStatic(DewdropReflectionUtils.class)) {
            readModelWrapper.callEventHandlers(new DewdropFundsAddedToAccount(UUID.randomUUID(), new BigDecimal(100)));
            utilities.verify(() -> DewdropReflectionUtils.callMethod(any(), any(Method.class), any(DewdropAccountCreated.class)), times(0));
        }
    }

    @Test
    @DisplayName("updateReadModelCache() - Given a cache, when we call updateReadModelCache(), then confirm we called ReadModelUtils.updateReadModelCacheField()")
    void updateReadModelCache() {
        ReadModelWrapper readModelWrapper = ReadModelWrapper.of(DewdropAccountDetailsReadModel.class).get();
        try (MockedStatic<ReadModelUtils> utilities = mockStatic(ReadModelUtils.class)) {
            readModelWrapper.updateReadModelCache(new HashMap<>());
            utilities.verify(() -> ReadModelUtils.updateReadModelCacheField(any(Field.class), any(), any(Map.class)), times(1));
        }
    }

    @Test
    @DisplayName("updateReadModelCache() - Given a ReadModel with no cache, when we call updateReadModelCache(), then confirm we do not call ReadModelUtils.updateReadModelCacheField()")
    void updateReadModelCache_noCacheField() {
        ReadModelWrapper readModelWrapper = ReadModelWrapper.of(DewdropAccountDetailsReadModel.class).get();
        readModelWrapper.setCacheField(Optional.empty());
        try (MockedStatic<ReadModelUtils> utilities = mockStatic(ReadModelUtils.class)) {
            readModelWrapper.updateReadModelCache(new HashMap<>());
            utilities.verify(() -> ReadModelUtils.updateReadModelCacheField(any(Field.class), any(), any(Map.class)), times(0));
        }
    }

    @Test
    @DisplayName("getSupportedEvents() - Given a ReadModelWrapper, when we call getSupportedEvents(), then confirm we return the supported events")
    void getSupportedEvents() {
        ReadModelWrapper readModelWrapper = ReadModelWrapper.of(DewdropAccountDetailsReadModel.class).get();
        assertThat(readModelWrapper.getSupportedEvents(), is(List.of(DewdropAccountCreated.class)));
    }


}


class SameAsDewdropAccountDetailsReadModel {
    Map<UUID, DewdropAccountDetails> cache;

    public void on(DewdropAccountCreated event) {

    }
}
