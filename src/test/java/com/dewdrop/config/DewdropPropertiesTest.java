package com.dewdrop.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DewdropPropertiesTest {
    @Test
    void connectionString() {
        String connectionString = "esdb://localhost:2113?tls=false";
        DewdropProperties dewdropProperties = DewdropProperties.builder()
                .connectionString(connectionString)
                .streamPrefix("")
                .create();
        assertEquals(connectionString, dewdropProperties.getConnectionString());
    }
}