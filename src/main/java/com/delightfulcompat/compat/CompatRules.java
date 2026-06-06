/*
 * Copyright 2026 Delightful Compat contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.delightfulcompat.compat;

import com.delightfulcompat.DelightfulCompat;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe in-memory holder for the loaded compatibility rules.
 *
 * <p>Populated twice over the mod lifecycle:</p>
 * <ol>
 *   <li><b>{@link #loadBundledDefaults()}</b> — called from the mod constructor so the data is
 *       available <i>immediately</i> (notably for the JEI plugin, which can register before any
 *       world/datapack is loaded). Reads the JSON shipped inside this mod's own jar.</li>
 *   <li><b>{@link #setGroups}/{@link #setConflicts}</b> — called by {@link CompatRuleManager} on
 *       every datapack (re)load, so datapacks/KubeJS/pack authors can OVERRIDE or ADD rules. This
 *       is authoritative once a world is loaded.</li>
 * </ol>
 */
public final class CompatRules {
    private CompatRules() {}

    private static final Gson GSON = new Gson();

    private static volatile List<UnificationGroup> groups = List.of();
    private static volatile List<RecipeConflict> conflicts = List.of();

    /**
     * Bundled rule files shipped in THIS jar. Keep in sync with the JSON under
     * {@code src/main/resources/data/delightful_compat/...}. The datapack reload is authoritative;
     * this list only seeds early defaults so JEI has something to work with at startup.
     */
    private static final String[] BUNDLED_UNIFICATION = {
        "/data/delightful_compat/unification/sweet_potato.json",
        "/data/delightful_compat/unification/dough.json",
        "/data/delightful_compat/unification/milk.json",
        "/data/delightful_compat/unification/mashed_potatoes.json",
        "/data/delightful_compat/unification/pancakes.json",
        "/data/delightful_compat/unification/toast.json",
    };

    private static final String[] BUNDLED_CONFLICTS = {
        "/data/delightful_compat/conflicts/dough_dupe_loop_ramadan.json",
    };

    public static List<UnificationGroup> groups() {
        return groups;
    }

    public static List<RecipeConflict> conflicts() {
        return conflicts;
    }

    public static void setGroups(List<UnificationGroup> g) {
        groups = List.copyOf(g);
        DelightfulCompat.LOGGER.info("[DelightfulCompat] {} unification group(s) loaded.", groups.size());
    }

    public static void setConflicts(List<RecipeConflict> c) {
        conflicts = List.copyOf(c);
        DelightfulCompat.LOGGER.info("[DelightfulCompat] {} recipe-conflict rule(s) loaded.", conflicts.size());
    }

    /** Loads the rule JSON bundled inside this mod's jar (best-effort, never throws). */
    public static void loadBundledDefaults() {
        groups = List.copyOf(readAll(BUNDLED_UNIFICATION, UnificationGroup.CODEC));
        conflicts = List.copyOf(readAll(BUNDLED_CONFLICTS, RecipeConflict.CODEC));
        DelightfulCompat.LOGGER.debug(
                "[DelightfulCompat] Bundled defaults: {} group(s), {} conflict(s).", groups.size(), conflicts.size());
    }

    private static <T> List<T> readAll(String[] resources, com.mojang.serialization.Codec<T> codec) {
        List<T> out = new ArrayList<>();
        for (String path : resources) {
            try (InputStream in = CompatRules.class.getResourceAsStream(path)) {
                if (in == null) {
                    DelightfulCompat.LOGGER.warn("[DelightfulCompat] Bundled rule not found on classpath: {}", path);
                    continue;
                }
                JsonElement json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
                codec.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(err -> DelightfulCompat.LOGGER.error(
                                "[DelightfulCompat] Failed to parse bundled rule {}: {}", path, err))
                        .ifPresent(out::add);
            } catch (Exception e) {
                DelightfulCompat.LOGGER.error("[DelightfulCompat] Error reading bundled rule {}", path, e);
            }
        }
        return out;
    }
}
