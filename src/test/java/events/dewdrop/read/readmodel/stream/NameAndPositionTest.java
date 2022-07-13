package events.dewdrop.read.readmodel.stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import events.dewdrop.structure.api.Event;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NameAndPositionTest {
    NameAndPosition nameAndPosition;
    StreamType streamType = StreamType.CATEGORY;
    String name = "name";
    Consumer<Event> eventConsumer = event -> {
    };

    @BeforeEach
    void setup() {
        nameAndPosition = NameAndPosition.builder().name(name).streamType(streamType).create();
    }


    @Test
    @DisplayName("Given a name, a streamType and a consumer, when we call the constructor, then the object is created")
    void constructor() {
        assertEquals(name, nameAndPosition.getName());
        assertEquals(streamType, nameAndPosition.getStreamType());
    }

    @Test
    @DisplayName("Given a nameAndPosition, when we call isComplete(), then isComplete() returns false")
    void isComplete() {
        assertEquals(false, nameAndPosition.isComplete());

    }

    @Test
    @DisplayName("Given a nameAndPosition, when we call completeTask(), then isComplete() returns true")
    void completeTask() {
        NameAndPosition result = nameAndPosition.completeTask("streamName", 1L);
        assertThat(nameAndPosition, is(result));
        assertEquals(true, nameAndPosition.isComplete());
    }
}
