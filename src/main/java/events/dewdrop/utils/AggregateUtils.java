package events.dewdrop.utils;

import events.dewdrop.aggregate.AggregateRoot;
import events.dewdrop.aggregate.annotation.Aggregate;
import events.dewdrop.structure.api.Command;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

@Log4j2
public class AggregateUtils {
    private AggregateUtils() {}

    private static final List<Class<?>> AGGREGATE_ROOTS_CACHE = new ArrayList<>();

    public static List<Class<?>> getAggregateRootsThatSupportCommand(Command command) {
        if (AGGREGATE_ROOTS_CACHE.isEmpty()) {
            getAnnotatedAggregateRoots();
        }

        List<Class<?>> result = new ArrayList<>();

        AGGREGATE_ROOTS_CACHE.forEach(aggregateRoot -> {
            if (MethodUtils.getMatchingAccessibleMethod(aggregateRoot, "handle", command.getClass()) != null) {
                result.add(aggregateRoot);
            }
        });

        if (CollectionUtils.isEmpty(result)) {
            log.error("No AggregateRoots found that have an @CommandHandler for handle({} command)", command.getClass().getSimpleName());
        }

        return result;
    }

    public static List<Class<?>> getAnnotatedAggregateRoots() {
        if (!AGGREGATE_ROOTS_CACHE.isEmpty()) { return AGGREGATE_ROOTS_CACHE; }

        Set<Class<?>> aggregates = DewdropAnnotationUtils.getAnnotatedClasses(Aggregate.class);

        aggregates.forEach(aggregate -> {
            log.info("Registering class annotated as @AggregateRoot {}", aggregate.getSimpleName());
            AGGREGATE_ROOTS_CACHE.add(aggregate);
        });

        if (CollectionUtils.isEmpty(AGGREGATE_ROOTS_CACHE)) {
            log.error("No AggregateRoots found - Make sure to annotate your aggregateRoots with @Aggregate");
        }
        return AGGREGATE_ROOTS_CACHE;
    }

    static void clear() {
        AGGREGATE_ROOTS_CACHE.clear();
    }

    public static Optional<AggregateRoot> create(Class<?> classToProxy) {
        // ClassPool pool = ClassPool.getDefault();
        try {
            // ProxyFactory factory = new ProxyFactory();
            // factory.setSuperclass(classToProxy);
            //
            // MethodHandler handler = new AggregateHandler<>();
            // Object instance = factory.create(null, null, handler);

            Object instance = ConstructorUtils.invokeConstructor(classToProxy);
            return Optional.of(new AggregateRoot(instance));
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            log.error("Failed to assign AggregateRoot", e);
            return Optional.empty();
        }
    }

    public static Optional<AggregateRoot> createFromCommandHandlerMethod(Method commandHandlerMethod) {
        Class<?> aggregateClass = CommandHandlerUtils.getAggregateRootClassFromCommandHandlerMethod(commandHandlerMethod);

        return create(aggregateClass);
    }
}
