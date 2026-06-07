# Changelog

All notable changes to **Delightful Compat** are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project
adheres to [Semantic Versioning](https://semver.org/).

## [1.3.8] - 2026-06-07

### Fixed (two reported "potato is a potato" gaps — both root-caused to untagged items)
- **Cucumber seeds are now compostable (and breed chickens/parrots).** Slavic Delight ships its own compost
  data map for `cucumber` (the crop, 0.65) but **omits `cucumber_seeds`**, and tags that seed into **nothing**
  — so it was invisible to everything keyed on `c:seeds` (our compost data map's `#c:seeds` entry, and the
  `#c:seeds`→`chicken_food`/`parrot_food` breeding merges). Fix: cross-populate `c:seeds` with
  `slavic_delight:cucumber_seeds` (new [c/tags/item/seeds.json](src/main/resources/data/c/tags/item/seeds.json)).
  One tag entry fixes **both** composting and breeding — and closes the still-open 1.3.6 "cucumber seeds won't
  breed chickens" gap, which never worked because the seed was never in `c:seeds`. Verified against the jars
  that every other supported-mod seed is already in `c:seeds`; cucumber was the only gap.
- **Peruvian's camote now subs for potato in recipes.** `c:crops/potato` contained only
  `veggiesdelight:sweet_potato`, so recipes keyed on it (which is why you saw "missing potato") accepted
  Veggies' sweet potato but rejected camote. Fix: merge the sweet-potato group (`#c:crops/sweet_potato`) into
  [c/tags/item/crops/potato.json](src/main/resources/data/c/tags/item/crops/potato.json). 1.3.7 had skipped
  this tag for cutting-safety, but since Veggies' sweet potato **already** lives there (the cutting overlap
  pre-exists) and you want sweet potato ≡ potato, parity wins. Trade-off: cutting a camote may now yield
  potato fries/diced instead of `camote_cortado` — same as Veggies' sweet potato already behaves.
- Both are pure datapack tag merges (no data-map changes). Guarded by new `DatapackIntegrityTest` cases.
- **Broader pattern:** these are the same class as before — a mod doesn't tag its item into the common tag our
  fixes rely on. The mechanisms (`c:seeds` for compost/breeding, tag-membership unification for ingredients)
  are systemic; remaining gaps are per-item. If another item misbehaves, it's untagged — name it and it's a
  one-line add.

## [1.3.7] - 2026-06-07

### Fixed (tag-membership unification — "potato is a potato")
- **Peruvian's camote now works wherever Veggies' sweet potato does.** Reported: camote couldn't be used in
  Farmer's Delight's Stuffed Pumpkin. Root cause: that recipe accepts any `c:foods/vegetable` (minus melon),
  and Veggies tags **its** sweet potato into `c:foods/vegetable` / `c:crops` / `minecraft:horse_food` /
  `villager_plantable_seeds`, but Peruvian's Delight barely tags `camote` at all — so the two "sweet potatoes"
  weren't actually interchangeable in tag-based recipes.
- **Fix:** merge the sweet-potato group tag (`#c:crops/sweet_potato`, which contains every sweet potato) into
  each of those cooking-safe category tags. Now camote (and any future sweet potato) is a first-class
  vegetable/crop, breeds horses & pigs, and is villager-plantable — same as Veggies'. Pure datapack.
- **Cooking-safety preserved:** `c:crops/potato` is deliberately **not** unified — it's a cutting-board input
  (potato fries / diced potatoes) and camote has its own cutting/cooking recipes, so merging there would make
  one input produce several outputs (the 1.3.0-class conflict).
- **Applied universally, not just to this case:** added `tools/audit_tag_unification.py`, which reads the
  addon jars and reports every group's category-tag gaps (flagging cooking-input tags to skip). Re-audited all
  groups — sweet potato was the one with real gaps; milk is already unified, and the rest only overlapped on
  cooking-unsafe / debuff / rarely-used tags, so they're correctly left alone.
- Guarded by a new `DatapackIntegrityTest` case. Verified on a dev server with all 12 addons loaded.

## [1.3.6] - 2026-06-06

### Fixed (animal breeding/feeding across all addons)
- **Modded seeds now breed chickens (and tame parrots).** Reported gap: chickens couldn't be bred with
  cucumber seeds. Root cause is the same shape as the compost bug — in 1.21.1 an animal's `isFood` reads a
  vanilla item tag `minecraft:<animal>_food`, but vanilla fills those tags with **hard-coded items**, not a
  common umbrella. `minecraft:chicken_food` lists only the 6 vanilla seeds, so any modded seed is rejected.
- **Fix:** merge the matching `c:` umbrella into each vanilla food tag (plain datapack tag-merge,
  `required:false` so it's harmless when a tag is empty/absent):
  - `minecraft:chicken_food` += `#c:seeds` — every tagged seed breeds chickens
  - `minecraft:parrot_food` += `#c:seeds` — every tagged seed tames parrots (parity)
  - `minecraft:pig_food` += `#c:foods/vegetable` — modded vegetables breed pigs (vanilla pig diet is a
    subset of this tag)
- Wheat-eaters (cow/sheep/goat/llama/horse) are intentionally left alone — no modded "grain" umbrella and no
  real gap. Pure datapack, no Java. Guarded by a new `DatapackIntegrityTest` case.
- **Note:** like the compost fix, an item only benefits if its mod tags it into `c:seeds` /
  `c:foods/vegetable`. If a specific item works for neither composting nor breeding, that mod didn't tag it —
  drop its jar in `libs-dev/` (or name the item) and it'll be added explicitly.

## [1.3.5] - 2026-06-06

### Fixed (compostability across all addons)
- **Seeds (and other plant matter) are compostable again.** Reported gap: cucumber seeds couldn't be put in
  a composter. Root cause is systemic — in NeoForge 1.21.1 `ComposterBlock` reads compostability from the
  **`neoforge:compostables` data map**; the old `ComposterBlock.COMPOSTABLES` Java map is deprecated and no
  longer consulted. Any Delight addon still registering compost values through that Java API silently has no
  effect, so its seeds/crops/leaves won't compost.
- **Fix:** a new data map (`data/neoforge/data_maps/item/compostables.json`) that merges with NeoForge's
  builtin and re-declares compostability by **common tag**, so it covers every addon at once and stays safe
  when a mod is absent (an empty tag just contributes nothing — no conditions needed):
  - `#c:seeds` → 0.3 (`can_villager_compost`)  ·  `#c:crops` → 0.65  ·  `#minecraft:saplings` → 0.3  ·
    `#minecraft:leaves` → 0.3 — all mirroring vanilla's own chances.
- Pure datapack, no Java. Guarded by a new `DatapackIntegrityTest` case.
- **Note:** this fixes any seed/plant that its mod tags into `c:seeds`/`c:crops` (the standard convention).
  If a specific item still won't compost, its mod didn't tag it — drop the jar in `libs-dev/` (or name the
  item) and it'll be added explicitly.

## [1.3.4] - 2026-06-06

### Added
- **Mod icon.** A 256px logo (`src/main/resources/delightfulcompat.png`, wired via `logoFile` in
  `neoforge.mods.toml`) so the mod shows an icon in the NeoForge mods list. It belongs to the same visual
  family as the sibling mods (bedrock-crafting-controls, bedrock-line-placement, partially-craftable-recipes):
  the shared dark rounded-square gradient frame and emerald/neutral palette, with its own unification motif —
  three neutral "duplicate" cells merging via converging arrows into one glowing emerald "canonical" cell.
  Regenerate with `python tools/make_icon.py` (requires Pillow).

## [1.3.3] - 2026-06-05

### Added (audit follow-up — bread tag unification)
- Cross-populated the two bread tags so bread is interchangeable across mods. The Delight mods split
  bread slices/toasts between `c:foods/bread` (Oak's, My Nether's — 30 recipes) and `c:bread_slices`
  (More Delight — 8 recipes), so e.g. More Delight's `bread_slice` didn't work in `c:foods/bread`
  recipes and Oak's didn't work in `c:bread_slices` recipes. Both tags now contain
  `moredelight:bread_slice`, `oaksdelight:bread_slice`, `mynethersdelight:slices_of_bread` and the
  three matching toasts. Pure tag membership — no recipe rewrites, so no conflict risk.

### Notes
- The audit's other suspected gap (`arbitrarydelight:pasta` → `c:foods/pasta`) was a **false positive**
  and intentionally NOT added: that item is a finished pasta *dish* (raw_pasta + tomato_sauce), whereas
  `c:foods/pasta` is the raw-pasta ingredient tag — adding it would let pasta dishes be cooked from
  pasta dishes. Duplicate *dishes* (carrot_cake, fries, popsicles, …) were left un-unified by request.

## [1.3.2] - 2026-06-05

### Fixed (systemic — from a full recipe audit)
- **Interchange no longer breaks furnace/smoker/campfire recipes.** A deterministic audit of all
  ~1280 Delight recipes found 15 real cooking conflicts, all from 1.3.0's interchange: rewriting
  single-input station recipes (e.g. `farmersdelight:bread_from_smelting` and
  `ramadandelight:flat_bread_from_smelting`) to read `c:foods/dough` made *every* dough smelt to
  multiple outputs (bread vs flat_bread vs pancakes), of which only one wins — so you couldn't reliably
  make the bread you wanted. This is the same class as the 1.3.1 sweet-potato fix, generalized:
  **ingredient interchange is now applied only to crafting-table recipes** (`crafting_shaped` /
  `crafting_shapeless`), where overlapping inputs coexist fine. Output canonicalization still applies
  everywhere (it never adds a recipe, so it can't conflict). 39 → 29 overrides; re-audit shows 0
  conflicts. The user-facing examples (toast-with-egg, cheese toastie, etc.) are crafting recipes and
  are unaffected.

## [1.3.1] - 2026-06-05

### Fixed (playtest feedback)
- **Sweet potato is no longer over-unified.** 1.3.0 made `peruviansdelight:camote` and
  `veggiesdelight:sweet_potato` interchangeable and collapsed one into the other — but they have
  DISTINCT cooked forms (cooked camote vs baked sweet potato), so collapsing put both smelting
  recipes on one input and only one was craftable. Reverted: sweet potato is removed from
  interchange/collapse (8 overrides dropped, 47→39) and its JEI hiding is turned off, so both crops
  stay distinct, visible, and separately cookable. They still share `c:crops/sweet_potato` for
  recipes natively written against that tag.
- **`oaksdelight:butter` is now craftable.** oaksdelight 1.0.9 ships no working butter recipe (the
  file at `cooking/butter.json` is a mislabeled duplicate of the muffins recipe), so butter — and
  everything needing it (pancakes, muffins, soft serve) — was impossible. Added a Cooking Pot recipe
  (2× milk → butter); muffins keep their own recipe.
- **Arbitrary Delight cheese now works as cheese.** `oaksdelight:cheese_toastie` (and other recipes)
  read `c:foods/cheese`, but that tag only contained `oaksdelight:cheese`. Added
  `arbitrarydelight:cheese_block` and `shredded_cheese` to the tag so any cheese satisfies them.

### Notes
- Reminder: Delight foods like butter, pancakes and muffins are made in the **Farmer's Delight Cooking
  Pot**, not the crafting grid — that's why JEI's right-click "+" (recipe transfer) does nothing for
  them. It's a station recipe, not a bug.

## [1.3.0] - 2026-06-05

### Added
- **Real ingredient interchange (recipe rewriting).** Until now, unification only *populated* `c:` tags —
  but the Delight addons' recipes hardcode specific items (e.g. `moredelight:toast_with_egg` literally
  requires `moredelight:toast`), so the tags went unread and the items were not actually interchangeable.
  This release overrides every consuming recipe across the installed Delight addons to read the group's
  `c:` tag instead of the hardcoded item, so **any equivalent item satisfies any recipe**. 47 recipe
  overrides across `farmersdelight`, `oaksdelight`, `ramadandelight`, `peruviansdelight`, `moredelight`,
  `slavic_delight`, `arbitrarydelight`, `veggiesdelight`. Each is self-gated (`mod_loaded` of the owning
  mod, plus `item_exists` of the canonical for output canonicalization) so it only loads when the original
  recipe would, and conversion recipes (e.g. `milk_bucket_from_bottles`) are detected and left untouched.
- **New `toast` unification group** — `oaksdelight:toast`, `moredelight:toast`, `mynethersdelight:toasts`
  unified via the new `c:foods/toast` tag (canonical: Oak's Delight). Every toast-consuming recipe (toast
  with egg/cheese/honey/…) now accepts any toast. Sixth unification group.
- **Output canonicalization** for the true-duplicate groups (sweet potato, toast, mashed potatoes, pancakes):
  recipes that produce a non-canonical variant are rewritten to output the canonical, so you stop
  accumulating duplicates. Deliberately NOT applied to milk (would break `milk_bucket` semantics) or dough
  (different sizes).

### Fixed
- `c:foods/dough` now explicitly lists `farmersdelight:wheat_dough` (previously relied on FD's nested
  `#c:foods/dough/wheat` tag), so dough interchange never depends on another mod's tag structure.

### Notes
- This makes the unification actually functional in-game rather than cosmetic. **JEI duplicate-hiding and
  the info pages still require the mod on the client**; recipe interchange itself is server-authoritative.
- Two items cannot be merged into literally one via datapack — both still exist; they are made
  interchangeable in recipes and the non-canonical is hidden in JEI.

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
