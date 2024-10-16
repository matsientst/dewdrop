package events.dewdrop.streamstore.stream;

import events.dewdrop.structure.StreamNameGenerator;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.UUID;

/**
 * The type Prefix stream name generator.
 */
public class PrefixStreamNameGenerator implements StreamNameGenerator {
    private final String prefix;

    /**
     * Instantiates a new Prefix stream name generator without a prefix
     */
    public PrefixStreamNameGenerator() {
        prefix = "";
    }

    /**
     * Instantiates a new Prefix stream name generator that will generate streamNames with a given
     * prefix.
     *
     * @param prefix the prefix
     */
    public PrefixStreamNameGenerator(String prefix) {
        this.prefix = StringUtils.isNotEmpty(prefix) ? prefix.toLowerCase(Locale.ROOT) : "";
    }

    /**
     * It takes the aggregate class name, and the ID and generates the stream name. For Example:
     * DewdropUserAggregate-fc19e182-045a-4f91-9c61-ae081383ed36
     *
     * @param aggregateName The class of the aggregate.
     * @param id            The id of the aggregate
     * @return A string that is the name of the stream.
     */
    @Override
    public String generateForAggregate(String aggregateName, UUID id) {
        StringBuilder builder = new StringBuilder();
        if (!StringUtils.startsWith(aggregateName, prefix)) {
            appendPrefix(builder);
        }
        builder.append(aggregateName);
        if (id != null) {
            builder.append("-").append(id);
        }
        return builder.toString();
    }

    private void appendPrefix(StringBuilder builder) {
        if (StringUtils.isNotEmpty(prefix)) {
            builder.append(prefix).append(".");
        }
    }

    /**
     * It takes the aggregate class returns a string that is the name of the category stream
     *
     * @param aggregateClass The class of the aggregate root.
     * @return The name of the aggregate class.
     */
    @Override
    public String generateForCategory(Class<?> aggregateClass) {
        return generateForCategory(aggregateClass.getSimpleName());
    }

    /**
     * It takes the aggregate class name and returns a string that is the name of the category stream
     *
     * @param aggregateClassName The name of the aggregate class.
     * @return A string that is the stream name of the category.
     */
    @Override
    public String generateForCategory(String aggregateClassName) {
        StringBuilder builder = new StringBuilder();
        builder.append("$ce").append("-");
        appendPrefix(builder);
        builder.append(aggregateClassName);
        return builder.toString();
    }

    /**
     * It takes the event class name and returns a string that is the name of the event stream
     *
     * @param aggregateClassName The class name of the event.
     * @return The event stream for the given event class name.
     */
    @Override
    public String generateForEvent(String aggregateClassName) {
        StringBuilder builder = new StringBuilder();
        builder.append("$et").append("-");
        // appendPrefix(builder);
        builder.append(aggregateClassName);
        return builder.toString();
    }

}
