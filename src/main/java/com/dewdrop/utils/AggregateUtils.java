package com.dewdrop.utils;

import com.dewdrop.aggregate.Aggregate;
import com.dewdrop.structure.api.Command;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

@Log4j2
public class AggregateUtils {
    private static final List<Class<?>> AGGREGATE_ROOTS_CACHE = new ArrayList<>();

    public static List<Class<?>> getAggregateRootsThatSupportCommand(Command command) {
        if(AGGREGATE_ROOTS_CACHE.isEmpty()) {
            getAnnotatedAggregateRoots();
        }

        List<Class<?>> result = new ArrayList<>();

        AGGREGATE_ROOTS_CACHE.forEach(aggregateRoot -> {
            if (MethodUtils.getMatchingAccessibleMethod(aggregateRoot, "handle", command.getClass()) != null) {
                result.add(aggregateRoot);
            }
        });

        if (CollectionUtils.isEmpty(result)) {
            log.error("No AggregateRoots found that have a method handle({} command)", command.getClass()
                .getSimpleName());
        }

        return result;
    }

    public static List<Class<?>> getAnnotatedAggregateRoots() {
        if (!AGGREGATE_ROOTS_CACHE.isEmpty()) {
            return AGGREGATE_ROOTS_CACHE;
        }

        Set<Class<?>> aggregates = AnnotationReflection.getAnnotatedClasses(Aggregate.class);

        aggregates.forEach(aggregate -> {
            AGGREGATE_ROOTS_CACHE.add(aggregate);
        });

        if (CollectionUtils.isEmpty(AGGREGATE_ROOTS_CACHE)) {
            log.error("No AggregateRoots found - Make sure to annotate your aggregateRoots with @Aggregate");
        }
        return AGGREGATE_ROOTS_CACHE;
    }
}
