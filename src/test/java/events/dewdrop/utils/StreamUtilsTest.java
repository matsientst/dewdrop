package events.dewdrop.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import events.dewdrop.read.readmodel.ReadModelWrapper;
import events.dewdrop.read.readmodel.annotation.Stream;
import events.dewdrop.read.readmodel.annotation.StreamStartPosition;
import events.dewdrop.read.readmodel.stream.StreamType;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import events.dewdrop.read.readmodel.ReadModel;
import events.dewdrop.structure.api.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class StreamUtilsTest {
    Method streamStartPositionMethod;
    Stream annotation;
    ReadModel<Event> readModel;
    ReadModelWrapper readModelWrapper;

    @BeforeEach
    void setup() throws NoSuchMethodException {
        streamStartPositionMethod = StreamStartPositionReadModel.class.getMethod("streamStartPosition");
        annotation = spy(StreamStartPositionReadModel.class.getAnnotation(Stream.class));
        readModel = mock(ReadModel.class);
        readModelWrapper = mock(ReadModelWrapper.class);
    }

    @Test
    @DisplayName("getStreamStartPositionMethod() - Given a stream annotation and a readModel, when there is a @StreamStartPosition annotation on a method, then return the method")
    void getStreamStartPositionMethod() {
        doReturn(readModelWrapper).when(readModel).getReadModelWrapper();
        doReturn(StreamStartPositionReadModel.class).when(readModelWrapper).getOriginalReadModelClass();
        doReturn(new StreamStartPositionReadModel()).when(readModelWrapper).getReadModel();
        try (MockedStatic<DewdropAnnotationUtils> utilities = mockStatic(DewdropAnnotationUtils.class)) {
            try (MockedStatic<DewdropReflectionUtils> reflectionUtils = mockStatic(DewdropReflectionUtils.class)) {
                utilities.when(() -> DewdropAnnotationUtils.getAnnotatedMethods(any(Class.class), any(Class.class))).thenReturn(Set.of(streamStartPositionMethod));
                reflectionUtils.when(() -> DewdropReflectionUtils.getMatchingMethod(any(Method.class), any(Class.class))).thenReturn(Optional.of(streamStartPositionMethod));


                Optional<Method> method = StreamUtils.getStreamStartPositionMethod(annotation.name(), annotation.streamType(), readModel);
                assertThat(method.isPresent(), is(true));
                assertThat(method.get(), is(method.get()));
            }
        }
    }

    @Test
    @DisplayName("getStreamStartPositionMethod() - Given a stream annotation and a readModel, when the ReadModelWrapper target class has a method that the ReadModel does not, then return Optional.empty()")
    void getStreamStartPositionMethod_noMatchingMethodOnTheReadModel() {
        ReadModel<Event> readModel = mock(ReadModel.class);
        ReadModelWrapper readModelWrapper = mock(ReadModelWrapper.class);
        doReturn(readModelWrapper).when(readModel).getReadModelWrapper();
        doReturn(StreamStartPositionReadModel.class).when(readModelWrapper).getOriginalReadModelClass();
        doReturn(new StreamStartPositionReadModel()).when(readModelWrapper).getReadModel();
        try (MockedStatic<DewdropAnnotationUtils> utilities = mockStatic(DewdropAnnotationUtils.class)) {
            try (MockedStatic<DewdropReflectionUtils> reflectionUtils = mockStatic(DewdropReflectionUtils.class)) {
                utilities.when(() -> DewdropAnnotationUtils.getAnnotatedMethods(any(Class.class), any(Class.class))).thenReturn(Set.of(streamStartPositionMethod));
                reflectionUtils.when(() -> DewdropReflectionUtils.getMatchingMethod(any(Method.class), any(Class.class))).thenReturn(Optional.empty());


                Optional<Method> method = StreamUtils.getStreamStartPositionMethod(annotation.name(), annotation.streamType(), readModel);
                assertThat(method.isEmpty(), is(true));
            }
        }
    }

    @Test
    @DisplayName("isCorrectStreamStartPosition() - Given a stream annotation and a method annotated with @StreamStartPosition, when they have the same name and stream type, then return true")
    void isCorrectStreamStartPosition() {
        assertThat(StreamUtils.isCorrectStreamStartPosition(annotation.name(), annotation.streamType(), streamStartPositionMethod), is(true));
    }


    @Test
    @DisplayName("isCorrectStreamStartPosition() - Given a stream annotation and a method annotated with @StreamStartPosition, when they do not have the same name, then return false")
    void isCorrectStreamStartPosition_wrongName() {
        doReturn("test").when(annotation).name();
        assertThat(StreamUtils.isCorrectStreamStartPosition(annotation.name(), annotation.streamType(), streamStartPositionMethod), is(false));
    }

    @Test
    @DisplayName("isCorrectStreamStartPosition() - Given a stream annotation and a method annotated with @StreamStartPosition, when they do not have the same streamType, then return false")
    void isCorrectStreamStartPosition_wrongStreamType() {
        doReturn("stream1").when(annotation).name();
        Mockito.doReturn(StreamType.AGGREGATE).when(annotation).streamType();
        assertThat(StreamUtils.isCorrectStreamStartPosition(annotation.name(), annotation.streamType(), streamStartPositionMethod), is(false));
    }

    @Stream(name = "stream1", streamType = StreamType.EVENT)
    public class StreamStartPositionReadModel {
        public StreamStartPositionReadModel() {}

        @StreamStartPosition(name = "stream1", streamType = StreamType.EVENT)
        public Long streamStartPosition() {
            return 10L;
        }
    }
}
