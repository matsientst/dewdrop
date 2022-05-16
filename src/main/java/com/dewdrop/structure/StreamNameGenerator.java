package com.dewdrop.structure;

import java.util.UUID;

public interface StreamNameGenerator {
    String generateForAggregate(Class<?> type, UUID id);

    String generateForCategory(Class<?> type);

    String generateForCategory(String category);

    String generateForEvent(String type);
}
