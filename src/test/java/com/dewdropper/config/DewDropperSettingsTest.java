package com.dewdropper.config;

import static org.junit.jupiter.api.Assertions.*;

import com.dewdropper.DewDropper;
import org.junit.jupiter.api.Test;

class DewDropperSettingsTest {
    @Test
    void construct() {
        DewDropperSettings dewDropperSettings = DewDropperSettings.builder()
                .properties(DewDropperProperties.builder()
                        .connectionString("esdb://localhost:2113?tls=false")
                        .streamPrefix("")
                        .create())
                .create();
        assertNotNull(dewDropperSettings);
    }
}