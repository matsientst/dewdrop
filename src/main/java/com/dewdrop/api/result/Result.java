package com.dewdrop.api.result;

import java.util.Objects;
import java.util.function.Consumer;

public class Result<T> {

    private static final Result<?> EMPTY = new Result<>();
    private final T value;
    private final Exception exception;

    private Result() {
        this.value = null;
        this.exception = null;
    }

    @SuppressWarnings("java:S112")
    public void rethrowRuntime() {
        if (exception != null) {throw new RuntimeException(exception);}
    }

    public void rethrow() throws ResultException {
        if (exception != null) {throw new ResultException(exception);}
    }

    public Exception getException() {
        return exception;
    }

    public static <T> Result<T> empty() {
        @SuppressWarnings("unchecked")
        Result<T> t = (Result<T>) EMPTY;
        return t;
    }

    private Result(T value) {
        this.value = Objects.requireNonNull(value);
        this.exception = null;
    }

    private Result(Exception exception) {
        this.exception = Objects.requireNonNull(exception);
        value = null;
    }

    public static <T> Result<T> of(T value) {
        return new Result<>(value);
    }

    public static <T> Result<T> of(Exception exception) {
        return new Result<>(exception);
    }

    public static <T> Result<T> ofException(Exception exception) {
        return exception == null ? empty() : of(exception);
    }

    public T get() throws ResultException {
        if (exception != null) {throw new ResultException(exception);}
        return value;
    }

    public boolean isValuePresent() {
        return value != null;
    }

    public boolean isExceptionPresent() {
        return exception != null;
    }

    public void ifPresent(Consumer<? super T> consumer) {
        if (exception == null) {
            consumer.accept(value);
        }
    }

    public boolean isEmpty() {
        return exception == null && value == null;
    }

    public void ifExceptionPresent(Consumer<? super Exception> consumer) {
        if (exception != null) {consumer.accept(exception);}
    }

    public void ifExceptionPresent(Class<? extends Exception> targetType, Consumer<? super Exception> consumer) {
        if (exception != null && targetType.isAssignableFrom(exception.getClass())) {
            consumer.accept(exception);
        }
    }

    public T orElse(T other) {
        if (exception == null) {return value == null ? other : value;}
        return other;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {return true;}
        if (!(obj instanceof Result)) {return false;}
        Result<?> other = (Result<?>) obj;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return exception == null ? String.format("Result[%s]", value) : String.format("Result[%s]", exception);
    }
}
