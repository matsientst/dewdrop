package com.dewdrop.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DewdropPropertiesTest {
    @Test
    void connectionString() {
        String connectionString = "esdb://localhost:2113?tls=false";
        DewdropProperties dewdropProperties = DewdropProperties.builder().connectionString(connectionString).packageToScan("com.dewdrop").streamPrefix("").create();
        assertEquals(connectionString, dewdropProperties.getConnectionString());
    }

    @Test
    @DisplayName("Package to scan is necessary to know where to find the annotated classes")
    void connectionString_noPackageToScan() {
        String connectionString = "esdb://localhost:2113?tls=false";
        assertThrows(IllegalArgumentException.class, () -> DewdropProperties.builder().connectionString(connectionString).streamPrefix("").create());

    }
}
