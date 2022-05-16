package com.dewdropper.utils;

import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

public class WaitUntilUtils {
    private WaitUntilUtils() {}

    public static void waitUntil(BooleanSupplier condition, long timeoutms) throws TimeoutException {
        long start = System.currentTimeMillis();
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() - start > timeoutms) { throw new TimeoutException(String.format("Condition not met within %s ms", timeoutms)); }
        }
    }
}
