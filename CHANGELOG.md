# Changelog

All notable changes to **Delightful Compat** are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project
adheres to [Semantic Versioning](https://semver.org/).

## [1.2.0] - 2026-06-05

### Added
- **Broken-recipe override layer** — a new use of the existing datapack-override mechanism that
  silences (or, where possible, fixes) parse errors logged by Delight addons whose recipes reference
  items, ingredients, or recipe serializers that aren't present. Each override is a verbatim copy of
  the upstream recipe with a `neoforge:conditions` gate, so it loads unchanged when the dependency
  exists and drops cleanly otherwise. 76 overrides across five addons, derived from a real modpack
  startup log:
  - **`farmersknives` (53)** — metal-knife recipes shipped without conditions; gated on
    `item_exists` of the output knife (which only registers when its metal mod is installed).
  - **`oaksdelight` (17)** — `*_display_case` crafting recipes whose output items oaksdelight 1.0.9
    never registers (orphaned recipes); gated on `item_exists` of the output.
  - **`oaksdelight` cleaver stubs (5)** — oaksdelight 1.0.9 ships `crafting/knives/*_cleaver.json` as
    EMPTY 0-byte files (`EOFException`) after relocating the real recipes to `cleaver/*`. Each empty
    stub is overridden with a valid copy of the real recipe gated by an always-false condition, so the
    error vanishes without registering a duplicate — the cleavers stay craftable via oaksdelight's own
    `cleaver/<material>` recipes (iron/gold/flint/diamond + netherite via smithing).
  - **`casualnessdelight` (4)** — deep-frying recipes shipped inside Peruvian's/More Delight that use
    the `casualness_delight:deep_frying` serializer; gated on `mod_loaded` + `item_exists`.
  - **`brewinandchewin` (1)** — a fermenting recipe shipped inside My Nether's Delight using the
    `brewinandchewin:fermenting` serializer; gated on `mod_loaded` + `item_exists`.
- Optional, `AFTER`-ordered dependencies on `farmersknives`, `casualness_delight`, and
  `brewinandchewin` so the overrides win datapack load order.

### Fixed
- **`peruviansdelight:masa_picarones`** — genuine repair, not just suppression. Upstream uses the
  deprecated 1.20-era `"item"` result key, which 1.21.1 rejects (`No key id`), losing the recipe.
  The override restores it with the corrected `"id"` key; all ingredients already exist.

### Notes
- This is a deliberate scope addition: Delightful Compat now also acts as a janitor for broken/orphaned
  Delight-ecosystem recipes, consistent with its crash-safe, condition-gated philosophy. Recipes that
  could never function (missing item/serializer) are dropped without log spam; nothing functional is
  removed. Out-of-ecosystem errors (e.g. `createdeco`, `crabbersdelight`) were left untouched.

## [1.1.0] - 2026-06-05

### Added
- Support for six more Delight addons (declared as **optional** dependencies, verified against their
  NeoForge 1.21.1 jars): Arbitrary Delight (`arbitrarydelight`), Veggies Delight (`veggiesdelight`),
  End's Delight (`ends_delight`), Chopper's Delight (`choppersdelight`), Chef's Delight (`chefsdelight`),
  My Nether's Delight (`mynethersdelight`). Twelve Delight mods are now supported.
- **Veggies Delight unification** (genuine new duplicates):
  - `veggiesdelight:sweet_potato` folded into the `sweet_potato` group (a second real sweet potato beside
    Peruvian's camote), unified via `c:crops/sweet_potato` + the new `c:foods/sweet_potato` tag.
  - `veggiesdelight:mashed_potatoes` folded into the `mashed_potatoes` group (now More + Slavic + Veggies).

### Fixed
- **JEI: never hide a group's last visible item.** Non-canonical duplicates are now hidden only when the
  group's canonical item actually exists; if the canonical's mod is absent, the remaining variants stay
  visible (e.g. mashed potatoes when More Delight isn't installed but Slavic/Veggies are).

### Notes
- Arbitrary, End's and My Nether's Delight add only distinct dishes (no base-ingredient duplicates) and
  interoperate through the common `c:` tags they already ship. Chef's and Chopper's Delight add no food
  items. All are wired as optional deps so they load safely and in order; no item-level rules were needed.

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
