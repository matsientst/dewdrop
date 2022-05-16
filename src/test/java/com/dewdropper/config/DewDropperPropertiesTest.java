package com.dewdropper.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DewDropperPropertiesTest {
    @Test
    void connectionString() {
        String connectionString = "esdb://localhost:2113?tls=false";
        DewDropperProperties dewDropperProperties = DewDropperProperties.builder()
                .connectionString(connectionString)
                .streamPrefix("")
                .create();
        assertEquals(connectionString, dewDropperProperties.getConnectionString());
    }
}