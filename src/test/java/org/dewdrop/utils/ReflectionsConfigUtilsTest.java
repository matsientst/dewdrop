package org.dewdrop.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReflectionsConfigUtilsTest {
    @Test
    @DisplayName("init() - Given a packageToScan, initialize the REFLECTIONS public static final")
    void init() {
        ReflectionsConfigUtils.init("org.dewdrop");
        assertThat(ReflectionsConfigUtils.REFLECTIONS, is(notNullValue()));
        assertThat(ReflectionsConfigUtils.EXCLUDE_PACKAGES.isEmpty(), is(true));
    }

    @Test
    @DisplayName("init() - Given no packages to scan, throw an IllegalArgumentException ")
    void init_noPackageToScan() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionsConfigUtils.init(""));
    }

    @Test
    @DisplayName("init() - Given a packageToScan and a list of excluded packages, initialize the REFLECTIONS public static final and confirm that the EXCLUDED_PACKAGES reflects exclusions")
    void init_exclusions() {
        ReflectionsConfigUtils.init("org.dewdrop", List.of("org.dewdrop"));
        assertThat(ReflectionsConfigUtils.REFLECTIONS, is(notNullValue()));
        assertThat(ReflectionsConfigUtils.EXCLUDE_PACKAGES.get(0), is("org.dewdrop"));
    }

}
