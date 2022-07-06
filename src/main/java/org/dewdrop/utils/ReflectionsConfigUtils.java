package org.dewdrop.utils;

import static org.reflections.scanners.Scanners.FieldsAnnotated;
import static org.reflections.scanners.Scanners.MethodsAnnotated;
import static org.reflections.scanners.Scanners.SubTypes;
import static org.reflections.scanners.Scanners.TypesAnnotated;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

public class ReflectionsConfigUtils {
    private ReflectionsConfigUtils() {}

    public static Reflections REFLECTIONS;
    public static List<String> EXCLUDE_PACKAGES = new ArrayList<>();

    public static void init(String packageToScan) {
        init(packageToScan, new ArrayList<>());
    }

    public static void init(String packageToScan, List<String> excludePackages) {
        if (StringUtils.isEmpty(packageToScan)) { throw new IllegalArgumentException("There is no package to scan for the annotations needed for dewdrop"); }
        EXCLUDE_PACKAGES = Optional.ofNullable(excludePackages).orElse(new ArrayList<>());
        FilterBuilder filters = new FilterBuilder();
        excludePackages.forEach(packageToExclude -> filters.excludePackage(packageToExclude));
        REFLECTIONS = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage(packageToScan)).filterInputsBy(filters).setScanners(FieldsAnnotated, MethodsAnnotated, TypesAnnotated, SubTypes));
    }

}
