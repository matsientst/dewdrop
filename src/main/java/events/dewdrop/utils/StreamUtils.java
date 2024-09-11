package events.dewdrop.utils;

import events.dewdrop.read.readmodel.ReadModelWrapper;
import events.dewdrop.read.readmodel.annotation.Stream;
import events.dewdrop.read.readmodel.annotation.StreamStartPosition;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;

import events.dewdrop.read.readmodel.stream.StreamType;
import org.apache.commons.lang3.StringUtils;
import events.dewdrop.read.readmodel.ReadModel;
import events.dewdrop.structure.api.Event;

public class StreamUtils {
    private StreamUtils() {}

    /**
     * "Find the method in the read model that is annotated with @StreamStartPosition and has the same
     * name and streamType as the stream annotation."
     *
     * @param <T> The type event supported by the ReadModel
     * @param streamName The name of the stream
     * @param streamType the type of stream
     * @param readModel The read model class
     * @return {@code Optional<Method>} - A method that is annotated with @StreamStartPosition and has
     *         the same name as the stream.
     */
    public static <T extends Event> Optional<Method> getStreamStartPositionMethod(String streamName, StreamType streamType, ReadModel<T> readModel) {
        final ReadModelWrapper readModelWrapper = readModel.getReadModelWrapper();
        Set<Method> annotatedFields = DewdropAnnotationUtils.getAnnotatedMethods(readModelWrapper.getOriginalReadModelClass(), StreamStartPosition.class);
        return annotatedFields.stream().filter(method -> isCorrectStreamStartPosition(streamName, streamType, method)).map(method -> {
            Optional<Method> matchingMethod = DewdropReflectionUtils.getMatchingMethod(method, readModelWrapper.getOriginalReadModelClass());
            if (matchingMethod.isPresent()) { return matchingMethod.get(); }
            return null;
        }).filter(Objects::nonNull).findAny();
    }

    /**
     * > If the name and streamType match the @StreamStartPosition annotations name and streamType, then
     * return true.
     *
     * @param streamName The name of the stream
     * @param streamType the type of stream
     * @param method The method that is being invoked.
     * @return boolean
     */
    static boolean isCorrectStreamStartPosition(String streamName, StreamType streamType, Method method) {
        StreamStartPosition fieldAnnotation = method.getAnnotation(StreamStartPosition.class);
        String fieldStreamName = fieldAnnotation.name();
        return StringUtils.equalsAnyIgnoreCase(fieldStreamName, streamName) && fieldAnnotation.streamType() == streamType;
    }
}
