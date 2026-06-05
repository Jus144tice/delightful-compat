/*
 * Copyright 2026 Delightful Compat contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.delightfulcompat.compat;

import com.delightfulcompat.DelightfulCompat;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Datapack reload listener that loads Delightful Compat's data-driven rule files.
 *
 * <p>One instance is registered per rule directory:</p>
 * <ul>
 *   <li>{@code data/<namespace>/unification/*.json} → {@link UnificationGroup}</li>
 *   <li>{@code data/<namespace>/conflicts/*.json}   → {@link RecipeConflict}</li>
 * </ul>
 *
 * <p>Because it scans <i>every</i> namespace, datapacks and other mods can drop additional
 * rule files into their own {@code unification/}/{@code conflicts/} folders and they will be
 * picked up automatically — this is the documented extension point (no recompile needed).</p>
 *
 * @param <T> the rule type produced by {@code codec}
 */
public final class CompatRuleManager<T> extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();

    private final Codec<T> codec;
    private final Consumer<List<T>> sink;
    private final String directory;

    private CompatRuleManager(String directory, Codec<T> codec, Consumer<List<T>> sink) {
        super(GSON, directory);
        this.directory = directory;
        this.codec = codec;
        this.sink = sink;
    }

    /** Listener for {@code unification/} rule files. */
    public static CompatRuleManager<UnificationGroup> unification() {
        return new CompatRuleManager<>("unification", UnificationGroup.CODEC, CompatRules::setGroups);
    }

    /** Listener for {@code conflicts/} rule files. */
    public static CompatRuleManager<RecipeConflict> conflicts() {
        return new CompatRuleManager<>("conflicts", RecipeConflict.CODEC, CompatRules::setConflicts);
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<T> parsed = new ArrayList<>();
        files.forEach((id, json) -> codec.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(err -> DelightfulCompat.LOGGER.error(
                        "[DelightfulCompat] Skipping invalid {} rule '{}': {}", directory, id, err))
                .ifPresent(parsed::add));
        sink.accept(parsed);
    }
}
