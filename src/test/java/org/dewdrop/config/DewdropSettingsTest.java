package org.dewdrop.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DewdropSettingsTest {
    @Test
    void construct() {
        DewdropProperties properties = DewdropProperties.builder().connectionString("esdb://localhost:2113?tls=false").streamPrefix("").packageToScan("org.dewdrop").create();
        DewdropSettings dewdropSettings = DewdropSettings.builder().properties(properties).create();
        assertNotNull(dewdropSettings);
    }

}
