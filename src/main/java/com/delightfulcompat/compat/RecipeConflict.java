/*
 * Copyright 2026 Delightful Compat contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.delightfulcompat.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * A data-driven description of a known recipe conflict and how it is resolved.
 *
 * <p>Loaded from {@code data/<namespace>/conflicts/<id>.json}. This record is primarily
 * <i>documentation/metadata</i>: the actual disabling happens by overriding the foreign
 * recipe JSON (with a {@code neoforge:false} condition) and the replacement recipes are
 * shipped under {@code data/delightful_compat/recipe/}. Java reads this so JEI can hide
 * the disabled recipes and so the conflict is discoverable/auditable.</p>
 *
 * <pre>{@code
 * {
 *   "id": "dough_dupe_loop_ramadan",
 *   "conflictingRecipes": [ "ramadandelight:small_dough_from_dough" ],
 *   "replacementRecipes": [ "delightful_compat:small_dough_from_wheat_dough_cutting" ],
 *   "requiredMods": [ "farmersdelight", "ramadandelight" ]
 * }
 * }</pre>
 *
 * @param id                 unique id for this conflict rule
 * @param conflictingRecipes recipe ids that are disabled (and should be hidden in JEI)
 * @param replacementRecipes recipe ids Delightful Compat adds to preserve the lost output
 * @param requiredMods       the conflict only applies when ALL these mods are present
 */
public record RecipeConflict(
        String id,
        List<ResourceLocation> conflictingRecipes,
        List<ResourceLocation> replacementRecipes,
        List<String> requiredMods) {
    public static final Codec<RecipeConflict> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                    Codec.STRING.fieldOf("id").forGetter(RecipeConflict::id),
                    ResourceLocation.CODEC
                            .listOf()
                            .optionalFieldOf("conflictingRecipes", List.of())
                            .forGetter(RecipeConflict::conflictingRecipes),
                    ResourceLocation.CODEC
                            .listOf()
                            .optionalFieldOf("replacementRecipes", List.of())
                            .forGetter(RecipeConflict::replacementRecipes),
                    Codec.STRING
                            .listOf()
                            .optionalFieldOf("requiredMods", List.of())
                            .forGetter(RecipeConflict::requiredMods))
            .apply(inst, RecipeConflict::new));
}
