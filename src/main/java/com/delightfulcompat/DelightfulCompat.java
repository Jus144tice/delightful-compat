/*
 * Copyright 2026 Delightful Compat contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.delightfulcompat;

import com.delightfulcompat.compat.CompatRuleManager;
import com.delightfulcompat.compat.CompatRules;
import com.delightfulcompat.compat.CompatValidator;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

/**
 * Delightful Compat — entry point.
 *
 * <p>A compatibility/unification layer for Farmer's Delight and its Delight addons. It adds
 * almost no content; instead it:</p>
 * <ul>
 *   <li>unifies equivalent ingredients via {@code c:} tags (datapack layer);</li>
 *   <li>resolves conflicting recipes, preferring to move one onto an FD station;</li>
 *   <li>provides milk-bucket fallbacks where a milk-bottle item is absent;</li>
 *   <li>cleans up JEI (optional) and validates the configured canonical items.</li>
 * </ul>
 *
 * <p>Everything is optional and guarded so a missing Delight mod never crashes the game.</p>
 */
@Mod(DelightfulCompat.MODID)
public final class DelightfulCompat {
    public static final String MODID = "delightful_compat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DelightfulCompat(IEventBus modBus, ModContainer container) {
        // Register the TOML config (config/delightful_compat-common.toml).
        container.registerConfig(ModConfig.Type.COMMON, DelightfulCompatConfig.SPEC);

        // Seed rule data from the bundled jar resources immediately, so the JEI plugin (which can
        // run before any world/datapack is loaded) always has something to work with. Datapack
        // reloads later override this via CompatRuleManager.
        CompatRules.loadBundledDefaults();

        // Mod-bus lifecycle: validate canonical/equivalent items once registries are frozen.
        modBus.addListener(this::onCommonSetup);

        // Game-bus: hook datapack reloads so packs/KubeJS can add or override compat rules.
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);

        LOGGER.info("[DelightfulCompat] Initialised. Ingredient unification + recipe conflict resolution active.");
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        // BuiltInRegistries.ITEM is fully populated and frozen by now.
        event.enqueueWork(CompatValidator::validate);
    }

    private void onAddReloadListeners(final AddReloadListenerEvent event) {
        event.addListener(CompatRuleManager.unification());
        event.addListener(CompatRuleManager.conflicts());
    }
}
