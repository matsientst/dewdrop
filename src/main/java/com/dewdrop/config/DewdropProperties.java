package com.dewdrop.config;

import lombok.Builder;
import lombok.Data;

@Data
public class DewdropProperties {

    private String connectionString;
    private String streamPrefix;
    private String packageToScan;

    @Builder(buildMethodName = "create")
    public DewdropProperties(String connectionString, String streamPrefix, String packageToScan) {
        this.connectionString = connectionString;
        this.streamPrefix = streamPrefix;
        this.packageToScan = packageToScan;
    }
}
