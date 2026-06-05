/*
 * Copyright 2026 Delightful Compat contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.delightfulcompat.integration.jei;

import com.delightfulcompat.DelightfulCompat;
import com.delightfulcompat.DelightfulCompatConfig;
import com.delightfulcompat.compat.CompatRules;
import com.delightfulcompat.compat.UnificationGroup;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Optional JEI integration.
 *
 * <p>This class is only ever instantiated by JEI itself (via the {@link JeiPlugin} annotation),
 * so when JEI is absent it is simply never touched — the core mod is wholly unaffected. The build
 * only compiles against the JEI API ({@code compileOnly}); there is no runtime hard-dependency.</p>
 *
 * <p>Two behaviours, both driven by {@link DelightfulCompatConfig} + the loaded {@link CompatRules}:</p>
 * <ul>
 *   <li><b>Hide duplicates</b> — non-canonical equivalents of each unification group are removed
 *       from the ingredient list ({@code enableJeiCleanup &amp;&amp; hideDuplicateItems}).</li>
 *   <li><b>Info pages</b> — each canonical item gets a JEI info page explaining the unification
 *       ({@code showCompatibilityNotes}).</li>
 * </ul>
 *
 * <p>Note: recipes disabled by Delightful Compat are removed at the datapack layer (they fail their
 * {@code neoforge:conditions}), so JEI already never shows them — {@code hideDisabledRecipes} is
 * satisfied without extra work here. The flag remains as the documented switch for any future
 * JEI-side recipe hiding.</p>
 */
@JeiPlugin
public class DelightfulCompatJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(DelightfulCompat.MODID, "jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (!jeiCleanupEnabled() || !DelightfulCompatConfig.SHOW_COMPATIBILITY_NOTES.get()) {
            return;
        }
        int pages = 0;
        for (UnificationGroup group : CompatRules.groups()) {
            Item canonical = resolve(group.canonical());
            if (canonical == null) {
                continue; // canonical's mod isn't installed -> nothing to annotate
            }
            registration.addIngredientInfo(
                    new ItemStack(canonical), VanillaTypes.ITEM_STACK, Component.translatable(group.jeiInfoKey()));
            pages++;
        }
        DelightfulCompat.LOGGER.info("[DelightfulCompat] JEI: added {} compatibility info page(s).", pages);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        if (!jeiCleanupEnabled() || !DelightfulCompatConfig.HIDE_DUPLICATE_ITEMS.get()) {
            return;
        }
        List<ItemStack> toHide = new ArrayList<>();
        for (UnificationGroup group : CompatRules.groups()) {
            if (!group.hideNonCanonicalInJei()) {
                continue;
            }
            for (ResourceLocation id : group.nonCanonicalEquivalents()) {
                Item item = resolve(id);
                if (item != null) {
                    toHide.add(new ItemStack(item));
                }
            }
        }
        if (!toHide.isEmpty()) {
            runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, toHide);
            DelightfulCompat.LOGGER.info(
                    "[DelightfulCompat] JEI: hid {} non-canonical duplicate item(s).", toHide.size());
        }
    }

    /** Master JEI switch; defensive against config not yet being loaded. */
    private static boolean jeiCleanupEnabled() {
        try {
            return DelightfulCompatConfig.ENABLE_JEI_CLEANUP.get();
        } catch (IllegalStateException notLoadedYet) {
            return false;
        }
    }

    /** @return the registered Item for {@code id}, or {@code null} if it does not exist. */
    private static Item resolve(ResourceLocation id) {
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return null;
        }
        return BuiltInRegistries.ITEM.get(id);
    }
}
