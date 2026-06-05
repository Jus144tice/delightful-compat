/*
 * Copyright 2026 Delightful Compat contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.delightfulcompat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Verifies the config spec builds and its defaults are sane. Runs on the NeoForge unit-test harness
 * because {@link DelightfulCompatConfig} touches {@code ModConfigSpec}. We assert {@code getDefault()}
 * (not {@code get()}) so we never need a loaded config file.
 */
class ConfigTest {

    @Test
    void specBuilds() {
        assertNotNull(DelightfulCompatConfig.SPEC, "config spec should build");
    }

    @Test
    void featureTogglesDefaultOn() {
        assertTrue(DelightfulCompatConfig.ENABLE_RECIPE_PATCHES.getDefault());
        assertTrue(DelightfulCompatConfig.ENABLE_INGREDIENT_UNIFICATION.getDefault());
        assertTrue(DelightfulCompatConfig.ENABLE_MILK_BUCKET_FALLBACKS.getDefault());
        assertTrue(DelightfulCompatConfig.ENABLE_JEI_CLEANUP.getDefault());
        assertTrue(DelightfulCompatConfig.HIDE_DUPLICATE_ITEMS.getDefault());
        assertTrue(DelightfulCompatConfig.HIDE_DISABLED_RECIPES.getDefault());
        assertTrue(DelightfulCompatConfig.SHOW_COMPATIBILITY_NOTES.getDefault());
    }

    @Test
    void canonicalDefaultsMatchTheShippedRules() {
        assertEquals("peruviansdelight:camote", DelightfulCompatConfig.CANONICAL_SWEET_POTATO_ITEM.getDefault());
        assertEquals("ramadandelight:small_dough", DelightfulCompatConfig.CANONICAL_SMALL_DOUGH_ITEM.getDefault());
        assertEquals("farmersdelight:wheat_dough", DelightfulCompatConfig.CANONICAL_LARGE_DOUGH_ITEM.getDefault());
    }
}
