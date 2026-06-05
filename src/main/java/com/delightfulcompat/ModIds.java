/*
 * Copyright 2026 Delightful Compat contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.delightfulcompat;

/**
 * Central registry of every mod-id and item-id Delightful Compat knows about.
 *
 * <p>THIS IS THE ONE PLACE TO EDIT when adding support for a new Delight addon.
 * Nothing here is "load-bearing" at runtime — these are convenience constants used
 * by the validator/JEI defaults and referenced by the JSON datapack files. Because
 * the datapack layer is guarded by {@code neoforge:conditions}, a wrong/unknown id
 * degrades gracefully (the patch is skipped) instead of crashing.</p>
 *
 * <p>All ids below were verified by extracting the actual NeoForge 1.21.1 jars from
 * Modrinth (mod-ids from {@code META-INF/neoforge.mods.toml}, item-ids from lang +
 * recipe/tag files). Where a "duplicate" item does not actually exist yet in this
 * mod set, the constant is marked as a forward-looking placeholder.</p>
 *
 * <h2>How to add a new Delight mod</h2>
 * <ol>
 *   <li>Add its mod-id constant under {@link Mods}.</li>
 *   <li>Add the relevant item-id constants under {@link Items}.</li>
 *   <li>Drop a {@code data/delightful_compat/unification/<group>.json} rule file
 *       (and extend the matching {@code c:} tag in {@code data/c/tags/item/...}).</li>
 *   <li>If a recipe collides, add a {@code conflicts/<id>.json} rule, an override that
 *       disables the original, and a replacement recipe (preferably on an FD station).</li>
 * </ol>
 * No Java recompile is needed for tags/recipes/rules — they are pure datapack JSON.
 */
public final class ModIds {
    private ModIds() {}

    /** Mod-ids (namespaces), verified from each addon's {@code neoforge.mods.toml}. */
    public static final class Mods {
        private Mods() {}

        public static final String FARMERS_DELIGHT = "farmersdelight";
        public static final String OAKS_DELIGHT = "oaksdelight";
        public static final String RAMADAN_DELIGHT = "ramadandelight";
        public static final String PERUVIANS_DELIGHT = "peruviansdelight";
        public static final String MORE_DELIGHT = "moredelight";
        public static final String SLAVIC_DELIGHT = "slavic_delight"; // NOTE the underscore
        // Second wave of supported addons.
        public static final String ARBITRARY_DELIGHT = "arbitrarydelight";
        public static final String VEGGIES_DELIGHT = "veggiesdelight";
        public static final String ENDS_DELIGHT = "ends_delight"; // NOTE the underscore
        public static final String CHOPPERS_DELIGHT = "choppersdelight";
        public static final String CHEFS_DELIGHT = "chefsdelight";
        public static final String MY_NETHERS_DELIGHT = "mynethersdelight";
        public static final String JEI = "jei";
    }

    /** Item-ids referenced by the bundled compatibility rules. */
    public static final class Items {
        private Items() {}

        // --- Sweet potato group -------------------------------------------------
        // Only Peruvian's Delight ships a sweet potato in this set ("camote").
        // It is therefore the canonical sweet potato. Other mods' sweet potatoes
        // (if present in a pack) should be added to data/c/tags/item/crops/sweet_potato.json.
        public static final String PERUVIAN_SWEET_POTATO = "peruviansdelight:camote";
        public static final String PERUVIAN_SWEET_POTATO_COOKED = "peruviansdelight:camote_cocido";
        // Veggies Delight adds a SECOND real sweet potato -> unified into the same group + tags.
        public static final String VEGGIES_SWEET_POTATO = "veggiesdelight:sweet_potato";
        // Forward-looking placeholders for sweet potatoes other mods are known to add.
        // They are listed (required:false) in the tag so they unify automatically IF present.
        public static final String CROPTOPIA_SWEET_POTATO = "croptopia:sweet_potato"; // placeholder
        public static final String VANILLA_NONEXISTENT_EXAMPLE = "minecraft:sweet_potato"; // does NOT exist (example)

        // --- Dough group --------------------------------------------------------
        // Farmer's Delight wheat_dough is canonical; Ramadan adds small_dough.
        public static final String FD_WHEAT_DOUGH = "farmersdelight:wheat_dough";
        public static final String RAMADAN_SMALL_DOUGH = "ramadandelight:small_dough";

        // --- Milk group ---------------------------------------------------------
        // FD's milk_bottle is canonical; vanilla milk_bucket is the universal fallback.
        public static final String FD_MILK_BOTTLE = "farmersdelight:milk_bottle";
        public static final String VANILLA_MILK_BUCKET = "minecraft:milk_bucket";

        // --- Mashed potatoes group (genuine cross-mod duplicate) ----------------
        // More Delight, Slavic Delight AND Veggies Delight each add a "Mashed Potatoes" item.
        public static final String MORE_MASHED_POTATOES = "moredelight:mashed_potatoes";
        public static final String SLAVIC_MASHED_POTATOES = "slavic_delight:mashed_potatoes";
        public static final String VEGGIES_MASHED_POTATOES = "veggiesdelight:mashed_potatoes";

        // --- Pancakes group (genuine cross-mod duplicate) -----------------------
        // BOTH Oaks Delight and Slavic Delight add a "Pancakes" item.
        public static final String OAKS_PANCAKES = "oaksdelight:pancakes";
        public static final String SLAVIC_PANCAKES = "slavic_delight:pancakes";
    }

    /** Common ({@code c:}) tags Delightful Compat unifies onto. */
    public static final class Tags {
        private Tags() {}

        // Existing tags various Delight mods already read from (we MERGE into them).
        public static final String C_FOODS_DOUGH = "c:foods/dough"; // Oaks/FD/Ramadan dough recipes
        public static final String C_FOODS_MILK = "c:foods/milk"; // Oaks pancakes & co. read this
        public static final String C_DRINKS_MILK = "c:drinks/milk"; // FD populates this with milk_bottle
        public static final String C_BUTTER = "c:butter"; // Oaks butter

        // New tag this mod introduces for the sweet potato group.
        public static final String C_CROPS_SWEET_POTATO = "c:crops/sweet_potato";
    }
}
