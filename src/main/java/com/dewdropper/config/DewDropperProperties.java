package com.dewdropper.config;

import lombok.Builder;
import lombok.Data;

@Data
public class DewDropperProperties {

    private String connectionString;
    private String streamPrefix;

    @Builder(buildMethodName = "create")
    public DewDropperProperties(String connectionString, String streamPrefix) {
        this.connectionString = connectionString;
        this.streamPrefix = streamPrefix;
    }
}
