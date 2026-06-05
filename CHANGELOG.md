# Changelog

All notable changes to **Delightful Compat** are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project
adheres to [Semantic Versioning](https://semver.org/).

## [1.0.0] - 2026-06-05

First public release for **Minecraft 1.21.1 / NeoForge 21.1.x**. A compatibility / unification layer
for Farmer's Delight and its Delight addons. Verified to boot clean on a dedicated server with Farmer's
Delight + Oaks + Ramadan + Peruvian + Slavic (5 unification groups load, the dough dupe-loop recipe is
disabled, zero Delightful-Compat datapack errors).

### Added
- **Ingredient unification** via common (`c:`) tags, with a data-driven rule system
  (`data/delightful_compat/unification/*.json`) loaded through a datapack reload listener (pack-overridable).
  Five groups ship: `sweet_potato`, `dough`, `milk`, `mashed_potatoes`, `pancakes`.
- **Milk fix + bucket fallback** — populate `c:foods/milk` / `c:drinks/milk` with `milk_bottle` and
  `minecraft:milk_bucket` so milk recipes (e.g. Oaks pancakes) work with a bucket, plus a conditional
  cake-from-bucket recipe that only loads when no milk-bottle item exists.
- **Dough conflict resolution** — Ramadan's `small_dough` is unified into `c:foods/dough`; the resulting
  `small_dough_from_dough` duplication loop is disabled and replaced with a Farmer's Delight cutting-board
  recipe (content preserved, no dupe).
- **Canonical item selection** per group, with config overrides and startup validation that logs a clear
  WARN (and falls back gracefully) when a configured/canonical item is not registered.
- **JEI integration** (optional) — hide non-canonical duplicate items and add per-group info pages.
- **Config** (`config/delightful_compat-common.toml`): `enableRecipePatches`, `enableIngredientUnification`,
  `enableMilkBucketFallbacks`, `enableJeiCleanup`, `hideDuplicateItems`, `hideDisabledRecipes`,
  `showCompatibilityNotes`, `canonicalSweetPotatoItem`, `canonicalSmallDoughItem`, `canonicalLargeDoughItem`.
- All six supported Delight mods (`farmersdelight`, `oaksdelight`, `ramadandelight`, `peruviansdelight`,
  `moredelight`, `slavic_delight`) declared as **optional** dependencies; every patch is guarded by
  `neoforge:conditions`, so any subset can be installed without crashes.
- JUnit test suite (config defaults, rule-Codec round-trips, datapack cross-file integrity) and Spotless
  (palantir-java-format) formatting, both folded into `./gradlew build`.
