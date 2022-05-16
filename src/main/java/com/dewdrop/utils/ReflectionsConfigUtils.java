package com.dewdrop.utils;

import static org.reflections.scanners.Scanners.FieldsAnnotated;
import static org.reflections.scanners.Scanners.MethodsAnnotated;
import static org.reflections.scanners.Scanners.TypesAnnotated;

import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

public class ReflectionsConfigUtils {
    public static Reflections REFLECTIONS;
    public static Reflections FIELDS_REFLECTIONS;
    public static Reflections METHODS_REFLECTIONS;
    public static Reflections TYPES_REFLECTIONS;


    public static void init(String packageToScan) {
        REFLECTIONS = new Reflections(packageToScan);
        FIELDS_REFLECTIONS = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage(packageToScan))
            .setScanners(FieldsAnnotated));
        METHODS_REFLECTIONS = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage(packageToScan))
            .setScanners(MethodsAnnotated));
        TYPES_REFLECTIONS = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage(packageToScan))
            .setScanners(TypesAnnotated));
    }

}
