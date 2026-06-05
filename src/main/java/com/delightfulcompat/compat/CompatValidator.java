/*
 * Copyright 2026 Delightful Compat contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.delightfulcompat.compat;

import com.delightfulcompat.DelightfulCompat;
import com.delightfulcompat.DelightfulCompatConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Startup sanity-checks. Runs once after item registration completes (FMLLoadCompleteEvent).
 *
 * <p>Never throws — every problem is logged and the mod continues with graceful fallback,
 * satisfying the "do not crash if a Delight mod is missing" requirement.</p>
 */
public final class CompatValidator {
    private CompatValidator() {}

    public static void validate() {
        DelightfulCompat.LOGGER.info("[DelightfulCompat] Validating compatibility rules against the item registry...");

        // 1) Validate every loaded unification group.
        for (UnificationGroup group : CompatRules.groups()) {
            if (!itemExists(group.canonical())) {
                // The group's own canonical is missing -> the providing mod isn't installed.
                // Not fatal (the group simply won't unify), but worth a clear notice.
                DelightfulCompat.LOGGER.warn(
                        "[DelightfulCompat] Group '{}' canonical item '{}' is not registered "
                                + "(its mod is likely not installed). The group will be inactive.",
                        group.group(),
                        group.canonical());
            }
            int present = 0;
            for (ResourceLocation eq : group.equivalents()) {
                if (itemExists(eq)) {
                    present++;
                } else {
                    // Expected whenever that addon isn't in the pack -> debug only, not a warning.
                    DelightfulCompat.LOGGER.debug(
                            "[DelightfulCompat] Group '{}': equivalent '{}' not present (skipped).", group.group(), eq);
                }
            }
            DelightfulCompat.LOGGER.info(
                    "[DelightfulCompat] Group '{}': {}/{} equivalent item(s) present.",
                    group.group(),
                    present,
                    group.equivalents().size());
        }

        // 2) Validate the CONFIGURED canonical items (the user-overridable ones).
        validateConfiguredCanonical(
                "canonicalSweetPotatoItem", DelightfulCompatConfig.CANONICAL_SWEET_POTATO_ITEM.get());
        validateConfiguredCanonical("canonicalSmallDoughItem", DelightfulCompatConfig.CANONICAL_SMALL_DOUGH_ITEM.get());
        validateConfiguredCanonical("canonicalLargeDoughItem", DelightfulCompatConfig.CANONICAL_LARGE_DOUGH_ITEM.get());
    }

    private static void validateConfiguredCanonical(String option, String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            DelightfulCompat.LOGGER.warn(
                    "[DelightfulCompat] Config option '{}' = '{}' is not a valid item id. Ignoring it.", option, value);
            return;
        }
        if (!itemExists(id)) {
            DelightfulCompat.LOGGER.warn(
                    "[DelightfulCompat] Config option '{}' points at '{}', which is NOT registered. "
                            + "Recipes will fall back to each rule's built-in canonical. "
                            + "Set this to an item from a mod that is actually installed.",
                    option,
                    id);
        } else {
            DelightfulCompat.LOGGER.info("[DelightfulCompat] Config canonical '{}' -> '{}' OK.", option, id);
        }
    }

    private static boolean itemExists(ResourceLocation id) {
        return id != null && BuiltInRegistries.ITEM.containsKey(id);
    }
}
