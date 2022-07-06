package org.dewdrop.utils;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.dewdrop.read.readmodel.ReadModel;
import org.dewdrop.read.readmodel.ReadModelWrapper;
import org.dewdrop.read.readmodel.annotation.Stream;
import org.dewdrop.read.readmodel.annotation.StreamStartPosition;
import org.dewdrop.structure.api.Event;

public class StreamUtils {
    private StreamUtils() {}

    /**
     * "Find the method in the read model that is annotated with @StreamStartPosition and has the same
     * name and streamType as the stream annotation."
     *
     * @param streamAnnotation The annotation on the read model class
     * @param readModel The read model class
     * @return A method that is annotated with @StreamStartPosition and has the same name as the stream.
     */
    public static <T extends Event> Optional<Method> getStreamStartPositionMethod(Stream streamAnnotation, ReadModel<T> readModel) {
        final ReadModelWrapper readModelWrapper = readModel.getReadModelWrapper();
        Set<Method> annotatedFields = DewdropAnnotationUtils.getAnnotatedMethods(readModelWrapper.getOriginalReadModelClass(), StreamStartPosition.class);
        return annotatedFields.stream().filter(method -> isCorrectStreamStartPosition(streamAnnotation, method)).map(method -> {
            Optional<Method> matchingMethod = DewdropReflectionUtils.getMatchingMethod(method, readModelWrapper.getReadModel());
            if (matchingMethod.isPresent()) { return matchingMethod.get(); }
            return null;
        }).filter(Objects::nonNull).findAny();
    }

    /**
     * > If the @Stream annotations name and streamType match the @StreamStartPosition annotations name
     * and streamType, then return true.
     *
     * @param streamAnnotation The @Stream annotation on the class
     * @param method The method that is being invoked.
     * @return A boolean value.
     */
    static boolean isCorrectStreamStartPosition(Stream streamAnnotation, Method method) {
        StreamStartPosition fieldAnnotation = method.getAnnotation(StreamStartPosition.class);
        String fieldStreamName = fieldAnnotation.name();
        String streamName = streamAnnotation.name();
        return StringUtils.equalsAnyIgnoreCase(fieldStreamName, streamName) && fieldAnnotation.streamType() == streamAnnotation.streamType();
    }
}
