package com.dewdrop.api.result;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;


class ResultTest {
    @Test
    void testSimpleValuePresent() throws ResultException {
        Result<String> value = Result.of("Value");
        assertTrue(value.isValuePresent());
        assertFalse(value.isExceptionPresent());
        assertEquals("Value", value.get());
    }

    @Test
    void testSimpleValuePresent_null() {
        Result<String> value = Result.empty();
        assertFalse(value.isValuePresent());
        assertFalse(value.isExceptionPresent());
    }

    @Test
    void testIfPresentWithValue() {
        Result<String> value = Result.of("Value");
        StringBuilder result = new StringBuilder();
        value.ifPresent(result::append);

        assertEquals("Value", result.toString());
    }

    @Test
    void testIfPresentWithoutValue() {
        Result<String> value = Result.of(new IllegalArgumentException("12345"));

        StringBuilder result = new StringBuilder();
        value.ifPresent(result::append);

        assertEquals("", result.toString());
    }

    @Test
    void testExceptionIsPresent() {
        Result<String> value = Result.of(new IllegalArgumentException("12345"));
        assertTrue(value.isExceptionPresent());
        assertThrows(ResultException.class, value::get);
    }


    @Test
    void testIfAnyExceptionIsPresent() {
        Result<String> value = Result.of(new IllegalArgumentException("12345"));
        StringBuilder result = new StringBuilder();
        value.ifExceptionPresent(ex -> result.append(ex.getMessage()));
        assertEquals("12345", result.toString());
    }

    @Test
    void testIfAnyExceptionIsPresent_noException() {
        Result<String> value = Result.of("test");
        StringBuilder result = new StringBuilder();
        value.ifExceptionPresent(ex -> result.append(ex.getMessage()));

        assertEquals("", result.toString());
    }

    @Test
    void testIfOneTypeOfExceptionIsNotPresent() {
        Result<String> value = Result.of(new IllegalArgumentException("12345"));

        StringBuilder result = new StringBuilder();
        value.ifExceptionPresent(IOException.class, ex -> result.append(ex.getMessage()));

        assertEquals("", result.toString());
    }

    @Test
    void testIfOneTypeOfExceptionIsPresent() {
        Result<String> value = Result.of(new IOException("12345"));
        StringBuilder result = new StringBuilder();
        value.ifExceptionPresent(IOException.class, ex -> result.append(ex.getMessage()));
        assertEquals("12345", result.toString());
    }


    @Test
    void testIfExceptionIsNotPresent() {
        Result<String> value = Result.of("Value");
        StringBuilder result = new StringBuilder();
        value.ifExceptionPresent(IOException.class, ex -> result.append(ex.getMessage()));
        assertEquals("", result.toString());
    }


    @Test
    void testOrElseValue() {
        Result<String> value = Result.of("Value");
        assertEquals("Value", value.orElse("Alternative Value"));
    }

    @Test
    void testOrElseAlternativeValue() {
        Result<String> value = Result.of(new IllegalArgumentException("12345"));
        assertEquals("Alternative Value", value.orElse("Alternative Value"));
    }

    @Test
    void testOrElseAlternativeValueEmpty() {
        Result<String> value = Result.empty();
        assertEquals("Alternative Value", value.orElse("Alternative Value"));
    }

    @Test
    void ofException() {
        assertEquals(Result.empty(), Result.ofException(new RuntimeException()));

        Result<Object> value = Result.of(new RuntimeException());
        assertEquals(value, Result.ofException(new RuntimeException()));

        assertEquals(Result.empty(), Result.ofException(null));
    }

    @Test
    void getException() {
        Exception exception = new RuntimeException();
        assertEquals(exception, Result.ofException(exception).getException());
    }

    @Test
    void rethrowRuntime() {
        Exception exception = new IllegalArgumentException();
        Result<Object> result = Result.ofException(exception);
        assertThrows(RuntimeException.class, result::rethrowRuntime);
        assertDoesNotThrow(() -> Result.of("test").rethrowRuntime());
    }

    @Test
    void rethrow() {
        Exception exception = new IllegalArgumentException();

        try {
            Result.ofException(exception).rethrow();
            fail("ResultException should have been thrown");
        } catch (Exception e) {
            assertTrue(e instanceof ResultException);
        }

        assertDoesNotThrow(() -> Result.of("test").rethrow());
    }

    @Test
    void isExceptionPresent() {
        String test = "test";
        Result<String> value = Result.of(test);
        assertFalse(value.isExceptionPresent());

        value = Result.of(new RuntimeException());
        assertTrue(value.isExceptionPresent());
    }

    @Test
    void isEmpty() {
        String test = "test";
        Result<String> value = Result.of(test);
        assertFalse(value.isEmpty());

        value = Result.empty();
        assertTrue(value.isEmpty());

        value = Result.of(new RuntimeException());
        assertFalse(value.isEmpty());
    }

    @Test
    void equalsTest() {
        String test = "test";
        Result<String> value = Result.of(test);
        assertEquals(value, value);
        assertEquals(value, Result.of(test));
        assertNotEquals(value, Optional.of(test));
    }

    @Test
    void toStringTest() {
        Result<String> value = Result.of("test");
        assertEquals("Result[test]", value.toString());

        value = Result.of(new RuntimeException());
        assertEquals("Result[java.lang.RuntimeException]", value.toString());
    }

    @Test
    void hashcodeTest() {
        String test = "test";
        Result<String> value = Result.of(test);
        assertEquals(test.hashCode(), value.hashCode());
    }
}
