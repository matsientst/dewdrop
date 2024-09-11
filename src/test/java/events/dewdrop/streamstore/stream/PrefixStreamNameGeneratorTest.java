package events.dewdrop.streamstore.stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import events.dewdrop.fixture.automated.DewdropUserAggregate;
import events.dewdrop.fixture.events.DewdropUserCreated;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PrefixStreamNameGeneratorTest {
    PrefixStreamNameGenerator nameGenerator;
    PrefixStreamNameGenerator prefixedNameGenerator;

    @BeforeEach
    void setup() {
        nameGenerator = new PrefixStreamNameGenerator();
        prefixedNameGenerator = new PrefixStreamNameGenerator("test");
    }

    @Test
    void generateForCategory() {
        assertThat(nameGenerator.generateForCategory(DewdropUserAggregate.class), is("$ce-DewdropUserAggregate"));
        assertThat(prefixedNameGenerator.generateForCategory(DewdropUserAggregate.class), is("$ce-test.DewdropUserAggregate"));
    }

    @Test
    void generateForAggregate() {
        UUID id = UUID.randomUUID();
        assertThat(nameGenerator.generateForAggregate(DewdropUserAggregate.class.getSimpleName(), id), is("DewdropUserAggregate-" + id));
        assertThat(prefixedNameGenerator.generateForAggregate(DewdropUserAggregate.class.getSimpleName(), id), is("test.DewdropUserAggregate-" + id));
    }

    @Test
    @DisplayName("Prefix is not honored for events in eventstore")
    void generateForEvent() {
        assertThat(nameGenerator.generateForEvent(DewdropUserCreated.class.getSimpleName()), is("$et-DewdropUserCreated"));
        assertThat(prefixedNameGenerator.generateForEvent(DewdropUserCreated.class.getSimpleName()), is("$et-DewdropUserCreated"));
    }
}
