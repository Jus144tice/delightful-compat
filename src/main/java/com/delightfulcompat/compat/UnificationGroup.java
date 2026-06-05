/*
 * Copyright 2026 Delightful Compat contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.delightfulcompat.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * A data-driven "these items are equivalent" rule.
 *
 * <p>Loaded from {@code data/<namespace>/unification/<name>.json}. Example:</p>
 * <pre>{@code
 * {
 *   "group": "sweet_potato",
 *   "canonical": "peruviansdelight:camote",
 *   "equivalents": [ "peruviansdelight:camote", "croptopia:sweet_potato" ],
 *   "tag": "c:crops/sweet_potato",
 *   "hideNonCanonicalInJei": true
 * }
 * }</pre>
 *
 * @param group                 human-readable group key (also used for the JEI info-page lang key)
 * @param canonical             the item NEW recipes should output / the item kept visible in JEI
 * @param equivalents           every known item that should be treated as this ingredient
 * @param tag                   optional {@code c:} tag that recipes use instead of hard item ids
 * @param hideNonCanonicalInJei whether the non-canonical equivalents may be hidden from JEI
 */
public record UnificationGroup(
        String group,
        ResourceLocation canonical,
        List<ResourceLocation> equivalents,
        Optional<ResourceLocation> tag,
        boolean hideNonCanonicalInJei) {
    public static final Codec<UnificationGroup> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                    Codec.STRING.fieldOf("group").forGetter(UnificationGroup::group),
                    ResourceLocation.CODEC.fieldOf("canonical").forGetter(UnificationGroup::canonical),
                    ResourceLocation.CODEC
                            .listOf()
                            .optionalFieldOf("equivalents", List.of())
                            .forGetter(UnificationGroup::equivalents),
                    ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(UnificationGroup::tag),
                    Codec.BOOL
                            .optionalFieldOf("hideNonCanonicalInJei", true)
                            .forGetter(UnificationGroup::hideNonCanonicalInJei))
            .apply(inst, UnificationGroup::new));

    /** Translation key for this group's JEI info page, e.g. {@code delightful_compat.jei.sweet_potato}. */
    public String jeiInfoKey() {
        return "delightful_compat.jei." + group;
    }

    /** The non-canonical equivalents (candidates for JEI hiding). */
    public List<ResourceLocation> nonCanonicalEquivalents() {
        return equivalents.stream().filter(id -> !id.equals(canonical)).toList();
    }
}
