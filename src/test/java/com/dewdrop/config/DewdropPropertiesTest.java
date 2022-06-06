package com.dewdrop.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DewdropPropertiesTest {
    @Test
    void connectionString() {
        String connectionString = "esdb://localhost:2113?tls=false";
        String packageToScan = "com.dewdrop";
        String packageToExclude = "com.dewdrop.fixture.customized";

        DewdropProperties dewdropProperties = DewdropProperties.builder().connectionString(connectionString).packageToScan(packageToScan).packageToExclude(packageToExclude).streamPrefix("").create();

        assertEquals(connectionString, dewdropProperties.getConnectionString());
        assertEquals(packageToScan, dewdropProperties.getPackageToScan());
        assertEquals(packageToExclude, dewdropProperties.getPackageToExclude().get(0));
    }
}
