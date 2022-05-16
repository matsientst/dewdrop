package com.dewdrop.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DewdropSettingsTest {
    @Test
    void construct() {
        DewdropSettings dewdropSettings = DewdropSettings.builder()
                .properties(DewdropProperties.builder()
                        .connectionString("esdb://localhost:2113?tls=false")
                        .streamPrefix("")
                        .create())
                .create();
        assertNotNull(dewdropSettings);
    }
}