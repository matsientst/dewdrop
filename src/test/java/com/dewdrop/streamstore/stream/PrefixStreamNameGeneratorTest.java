package com.dewdrop.streamstore.stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.dewdrop.fixture.automated.DewdropUserAggregate;
import com.dewdrop.fixture.events.DewdropUserCreated;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
        assertThat(prefixedNameGenerator.generateForCategory(DewdropUserAggregate.class), is("$ce-Test-DewdropUserAggregate"));
    }

    @Test
    void generateForAggregate() {
        UUID id = UUID.randomUUID();
        assertThat(nameGenerator.generateForAggregate(DewdropUserAggregate.class, id), is("DewdropUserAggregate-" + id));
        assertThat(prefixedNameGenerator.generateForAggregate(DewdropUserAggregate.class, id), is("Test-DewdropUserAggregate-" + id));
    }

    @Test
    void generateForEvent() {
        assertThat(nameGenerator.generateForEvent(DewdropUserCreated.class.getSimpleName()), is("$et-DewdropUserCreated"));
        assertThat(prefixedNameGenerator.generateForEvent(DewdropUserCreated.class.getSimpleName()), is("$et-Test-DewdropUserCreated"));
    }
}
