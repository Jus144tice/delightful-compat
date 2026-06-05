/*
 * Copyright 2026 Delightful Compat contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.delightfulcompat;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Delightful Compat configuration (NeoForge TOML, COMMON type — same values on client &amp; server).
 *
 * <p>Generated at {@code config/delightful_compat-common.toml} on first run.</p>
 *
 * <h2>What each toggle actually controls</h2>
 * <ul>
 *   <li><b>JEI / validation toggles</b> ({@code enableJeiCleanup}, {@code hideDuplicateItems},
 *       {@code hideDisabledRecipes}, {@code showCompatibilityNotes}, canonical item ids) are read
 *       directly by the Java/JEI layer and take effect immediately.</li>
 *   <li><b>Datapack toggles</b> ({@code enableRecipePatches}, {@code enableIngredientUnification},
 *       {@code enableMilkBucketFallbacks}): the underlying tag/recipe JSON is gated by mod/item
 *       <i>presence</i> via {@code neoforge:conditions} (NeoForge has no built-in "config value"
 *       recipe condition). These flags are honored by the Java/JEI/validation layer and are the
 *       documented switch for pack authors who additionally want to disable the bundled patch
 *       datapack. See README "Limitations".</li>
 * </ul>
 */
public final class DelightfulCompatConfig {
    private DelightfulCompatConfig() {}

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // ---- Feature master switches -------------------------------------------------
    public static final ModConfigSpec.BooleanValue ENABLE_RECIPE_PATCHES;
    public static final ModConfigSpec.BooleanValue ENABLE_INGREDIENT_UNIFICATION;
    public static final ModConfigSpec.BooleanValue ENABLE_MILK_BUCKET_FALLBACKS;

    // ---- JEI cleanup -------------------------------------------------------------
    public static final ModConfigSpec.BooleanValue ENABLE_JEI_CLEANUP;
    public static final ModConfigSpec.BooleanValue HIDE_DUPLICATE_ITEMS;
    public static final ModConfigSpec.BooleanValue HIDE_DISABLED_RECIPES;
    public static final ModConfigSpec.BooleanValue SHOW_COMPATIBILITY_NOTES;

    // ---- Canonical item selection ------------------------------------------------
    public static final ModConfigSpec.ConfigValue<String> CANONICAL_SWEET_POTATO_ITEM;
    public static final ModConfigSpec.ConfigValue<String> CANONICAL_SMALL_DOUGH_ITEM;
    public static final ModConfigSpec.ConfigValue<String> CANONICAL_LARGE_DOUGH_ITEM;

    public static final ModConfigSpec SPEC;

    static {
        B.comment("Delightful Compat — Farmer's Delight addon unification & conflict resolution")
                .push("general");

        ENABLE_RECIPE_PATCHES = B.comment(
                        "Master switch for recipe conflict resolution (disable + replacement recipes).",
                        "Honored by the JEI/validation layer. The datapack patches are additionally",
                        "guarded by neoforge:conditions on mod/item presence.")
                .define("enableRecipePatches", true);

        ENABLE_INGREDIENT_UNIFICATION = B.comment(
                        "Master switch for ingredient unification (merging equivalent items into c: tags).")
                .define("enableIngredientUnification", true);

        ENABLE_MILK_BUCKET_FALLBACKS = B.comment(
                        "Allow minecraft:milk_bucket to satisfy 'milk' ingredients when a mod's",
                        "milk-bottle item is absent (realized via the c:foods/milk + c:drinks/milk tags).")
                .define("enableMilkBucketFallbacks", true);

        B.pop();
        B.comment("JEI (Just Enough Items) cleanup — only applies when JEI is installed.")
                .push("jei");

        ENABLE_JEI_CLEANUP =
                B.comment("Master switch for all JEI tweaks below.").define("enableJeiCleanup", true);

        HIDE_DUPLICATE_ITEMS = B.comment(
                        "Hide non-canonical duplicate items (the 'equivalents' of each unification",
                        "group, minus the canonical one) from the JEI ingredient list.")
                .define("hideDuplicateItems", true);

        HIDE_DISABLED_RECIPES = B.comment(
                        "Hint that recipes disabled by Delightful Compat should not appear in JEI.",
                        "Disabled recipes are removed at the datapack layer, so JEI already omits them;",
                        "this flag governs any extra JEI-side hiding we perform.")
                .define("hideDisabledRecipes", true);

        SHOW_COMPATIBILITY_NOTES = B.comment("Add a JEI info page to each canonical item explaining the unification.")
                .define("showCompatibilityNotes", true);

        B.pop();
        B.comment(
                        "Canonical item ids. Set to the exact 'namespace:path' you want NEW recipes to output.",
                        "If the configured item does not exist, a clear WARN is logged and the rule's own",
                        "default canonical is used instead (graceful fallback — never a crash).")
                .push("canonical");

        CANONICAL_SWEET_POTATO_ITEM = B.comment("Canonical sweet potato. Default: Peruvian's Delight 'camote'.")
                .define("canonicalSweetPotatoItem", ModIds.Items.PERUVIAN_SWEET_POTATO);

        CANONICAL_SMALL_DOUGH_ITEM = B.comment("Canonical 'small' dough. Default: Ramadan Delight 'small_dough'.")
                .define("canonicalSmallDoughItem", ModIds.Items.RAMADAN_SMALL_DOUGH);

        CANONICAL_LARGE_DOUGH_ITEM = B.comment(
                        "Canonical 'large'/standard dough. Default: Farmer's Delight 'wheat_dough'.")
                .define("canonicalLargeDoughItem", ModIds.Items.FD_WHEAT_DOUGH);

        B.pop();
        SPEC = B.build();
    }
}
