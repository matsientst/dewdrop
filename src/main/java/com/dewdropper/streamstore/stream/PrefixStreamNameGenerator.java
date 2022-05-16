package com.dewdropper.streamstore.stream;

import com.dewdropper.structure.StreamNameGenerator;
import java.util.Locale;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;

public class PrefixStreamNameGenerator implements StreamNameGenerator {
    private final String prefix;

    public PrefixStreamNameGenerator() {
        prefix = "";
    }

    public PrefixStreamNameGenerator(String prefix) {
        this.prefix = StringUtils.isNotEmpty(prefix) ? prefix.toLowerCase(Locale.ROOT) : "";
    }

    @Override
    public String generateForAggregate(Class<?> type, UUID id) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotEmpty(prefix)) {
            builder.append(CaseUtils.toCamelCase(prefix, true)).append("-");
        }
        builder.append(type.getSimpleName()).append("-");
        builder.append(id);
        return builder.toString();
    }

    @Override
    public String generateForCategory(Class<?> type) {
        return generateForCategory(type.getSimpleName());
    }

    @Override
    public String generateForCategory(String category) {
        StringBuilder builder = new StringBuilder();
        builder.append("$ce").append("-");
        if (StringUtils.isNotEmpty(prefix)) {
            builder.append(CaseUtils.toCamelCase(prefix, true)).append("-");
        }
        builder.append(category);
        return builder.toString();
    }

    @Override
    public String generateForEvent(String type) {
        StringBuilder builder = new StringBuilder();
        builder.append("$et").append("-");
        builder.append(type);
        return builder.toString();
    }

}
