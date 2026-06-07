# CLAUDE.md — Delightful Compat

## What this mod is

**Delightful Compat** is a *compatibility / unification* mod for **Farmer's Delight** and its *Delight*
addons on **NeoForge 1.21.1**. It adds almost no content. It makes a stack of Delight addons coexist in
large modpacks by:

- **Unifying equivalent ingredients** through common (`c:`) item tags.
- **Picking a canonical item** per duplicate group (new/replacement recipes output only the canonical one).
- **Resolving recipe conflicts** — disable the colliding recipe, re-add the lost output, preferring to move
  one recipe onto a Farmer's Delight station (cutting board, cooking pot) rather than delete content.
- **Milk-bucket fallbacks** when a milk-bottle item is missing.
- **Suppressing broken/orphaned addon recipes** — overriding a foreign recipe with a `neoforge:conditions`
  gate so it loads only when it can actually work (a missing item/serializer otherwise spams parse errors).
- **Restoring compostability** — a `neoforge:compostables` data map makes tagged seeds/crops/plants
  compostable across all addons (many ship the deprecated Java registration the 1.21.1 composter ignores).
- **Universalizing animal food** — merging `c:` umbrellas into the vanilla `minecraft:<animal>_food` tags so
  modded seeds/veg breed the right animals (e.g. any seed breeds a chicken, not just the 6 vanilla ones).
- **JEI cleanup** — hide non-canonical duplicates, add info pages.

Everything is optional and crash-safe: every datapack patch is guarded by `neoforge:conditions`, and every
mod dependency is `optional`. A missing Delight mod just means the relevant patch is skipped.

**Supported mods** (12; all declared optional, verified by extracting the real jars): `farmersdelight`,
`oaksdelight`, `ramadandelight`, `peruviansdelight`, `moredelight`, `slavic_delight`, `arbitrarydelight`,
`veggiesdelight`, `ends_delight`, `choppersdelight`, `chefsdelight`, `mynethersdelight`. The last six were
added in 1.1.0; only `veggiesdelight` brought new duplicates (sweet potato + mashed potatoes) — the others
add distinct dishes / no items and interop via shared `c:` tags. JEI is an optional, compile-only integration.

Deeper prose lives in [README.md](README.md) (user-facing) and [IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md)
(deliverables/limitations). This file is the **AI navigation map** — keep it terse and accurate.

---

## ⚠️ SELF-UPDATING MANDATE (read every session)

**This file MUST be kept in sync, in the SAME session that changes the code — never deferred to "later".**

Whenever you make a *meaningful* change, update the relevant section of this file **before ending your turn**:

- add/remove/rename a Java class, public method, or config field → update [Codebase map](#codebase-map);
- add/change a unification group, conflict, tag, recipe, or lang key → update [Compat content](#compat-content)
  AND verify the cross-file checklist in [Editing recipes for common tasks](#editing-recipes-for-common-tasks);
- change a version (NeoForge / MC / JEI / Java / pack_format) or a build detail → update [Build & verify](#build--verify)
  and [gradle.properties](gradle.properties);
- learn a new invariant/gotcha (a thing that wasted time) → add it to [Invariants & gotchas](#invariants--gotchas).

A change is "meaningful" if a future reader would be misled, or would have to `grep`/read source to rediscover
it. Pure formatting or comment typos do not require an update. When unsure, update.

**Goal of this file: minimize future token generation, greps, and finds.** Prefer a precise pointer
(`File#symbol`) over pasting code. Do not add line numbers (they rot) — use symbol/filename anchors.

---

## Codebase map

Package root: `com.delightfulcompat` under [src/main/java/com/delightfulcompat/](src/main/java/com/delightfulcompat/).
Reference symbols as `File#member`.

### Java — core
- [DelightfulCompat.java](src/main/java/com/delightfulcompat/DelightfulCompat.java) — `@Mod` entry point.
  - `#MODID` (`"delightful_compat"`), `#LOGGER`.
  - ctor `(IEventBus, ModContainer)`: registers config, calls `CompatRules#loadBundledDefaults`, hooks
    `#onCommonSetup` (mod bus) + `#onAddReloadListeners` (game bus).
  - `#onCommonSetup(FMLCommonSetupEvent)` → `CompatValidator#validate` via `enqueueWork`.
  - `#onAddReloadListeners(AddReloadListenerEvent)` → registers `CompatRuleManager.unification()` + `.conflicts()`.
- [DelightfulCompatConfig.java](src/main/java/com/delightfulcompat/DelightfulCompatConfig.java) — TOML config
  (`#SPEC`, COMMON). Categories `general` / `jei` / `canonical`. Fields: `#ENABLE_RECIPE_PATCHES`,
  `#ENABLE_INGREDIENT_UNIFICATION`, `#ENABLE_MILK_BUCKET_FALLBACKS`, `#ENABLE_JEI_CLEANUP`,
  `#HIDE_DUPLICATE_ITEMS`, `#HIDE_DISABLED_RECIPES`, `#SHOW_COMPATIBILITY_NOTES`,
  `#CANONICAL_SWEET_POTATO_ITEM`, `#CANONICAL_SMALL_DOUGH_ITEM`, `#CANONICAL_LARGE_DOUGH_ITEM`.
- [ModIds.java](src/main/java/com/delightfulcompat/ModIds.java) — **single source** of mod/item/tag constants.
  Nested `Mods`, `Items`, `Tags`. **Edit here first when adding a mod/item.**

### Java — data-driven rules (`compat/`)
- [UnificationGroup.java](src/main/java/com/delightfulcompat/compat/UnificationGroup.java) — record
  `{group, canonical, equivalents, tag, hideNonCanonicalInJei}` + `#CODEC`, `#jeiInfoKey`, `#nonCanonicalEquivalents`.
- [RecipeConflict.java](src/main/java/com/delightfulcompat/compat/RecipeConflict.java) — record
  `{id, conflictingRecipes, replacementRecipes, requiredMods}` + `#CODEC`. (Metadata only; the actual disabling
  is done by the datapack override — see [Compat content](#compat-content).)
- [CompatRules.java](src/main/java/com/delightfulcompat/compat/CompatRules.java) — in-memory store.
  `#groups`/`#conflicts` getters, `#setGroups`/`#setConflicts` (called by reload listener),
  `#loadBundledDefaults` (reads jar resources for early JEI availability). **`#BUNDLED_UNIFICATION` /
  `#BUNDLED_CONFLICTS` arrays must list every bundled rule file** (see gotchas).
- [CompatRuleManager.java](src/main/java/com/delightfulcompat/compat/CompatRuleManager.java) — generic
  `SimpleJsonResourceReloadListener<T>`. Factories `#unification()` (dir `unification`) and `#conflicts()`
  (dir `conflicts`). `#apply` decodes via Codec, feeds the `CompatRules` setter.
- [CompatValidator.java](src/main/java/com/delightfulcompat/compat/CompatValidator.java) — `#validate`
  checks group canonicals/equivalents + the 3 configured canonical items against `BuiltInRegistries.ITEM`;
  WARN + graceful fallback, never throws. `#validateConfiguredCanonical`, `#itemExists`.

### Java — JEI (`integration/jei/`)
- [DelightfulCompatJeiPlugin.java](src/main/java/com/delightfulcompat/integration/jei/DelightfulCompatJeiPlugin.java)
  — `@JeiPlugin`. `#registerRecipes` (info pages, gated by `SHOW_COMPATIBILITY_NOTES`), `#onRuntimeAvailable`
  (hide non-canonical duplicates, gated by `HIDE_DUPLICATE_ITEMS`), `#jeiCleanupEnabled`, `#resolve`.
  Only ever loaded by JEI itself, so its absence can't affect the core mod.

### Tests (`src/test/java/com/delightfulcompat/`)
Run on the NeoForge `unitTest` harness (MC on the classpath). All Apache-headered.
- [ConfigTest.java](src/test/java/com/delightfulcompat/ConfigTest.java) — `DelightfulCompatConfig.SPEC`
  builds; toggles default on; canonical defaults match the shipped rules. Uses `getDefault()` (no loaded config).
- [compat/CompatRulesTest.java](src/test/java/com/delightfulcompat/compat/CompatRulesTest.java) — runs the real
  Codecs via `CompatRules#loadBundledDefaults`: exact 5-group name set, 1 conflict, canonical ∈ equivalents,
  every group has a tag. **This is the guard that `CompatRules.BUNDLED_*` matches the rule files.**
- [DatapackIntegrityTest.java](src/test/java/com/delightfulcompat/DatapackIntegrityTest.java) — pure (no MC):
  every unification group has a `delightful_compat.jei.<group>` lang key + an existing `c:` tag file; every
  conflict replacement recipe exists as a file; all `data/**/*.json` parse. **Catches the cross-file drift the
  "Editing recipes" checklist warns about.**

### Metadata / build
- [neoforge.mods.toml](src/main/resources/META-INF/neoforge.mods.toml) — mod id + all 6 addons & JEI as
  `optional` deps. `logoFile = "delightfulcompat.png"`.
- [delightfulcompat.png](src/main/resources/delightfulcompat.png) — 256px mod icon (the NeoForge mods-list
  logo). Generated by [tools/make_icon.py](tools/make_icon.py) (Pillow); shares the dark rounded-square
  gradient frame + emerald/neutral palette of the sibling mods (bedrock-crafting-controls,
  bedrock-line-placement, partially-craftable-recipes). Own motif: 3 neutral duplicate cells → converging
  arrows → 1 glowing emerald canonical cell (the unification idea). Re-run `python tools/make_icon.py` to
  regenerate.
- [pack.mcmeta](src/main/resources/pack.mcmeta) — `pack_format` 48.
- [build.gradle](build.gradle) — NeoForge block, optional JEI dep, the guarded `libs-dev/` dev-mod loader.
- [gradle.properties](gradle.properties) — all versions (MC, NeoForge, JEI, mod).
- [libs-dev/](libs-dev/) — drop addon jars here for `runClient`/`runServer` AND for `tools/audit_tag_unification.py`
  (never committed; see its README.txt). The full Delight set is fetchable from Modrinth for NeoForge 1.21.1.
- [tools/audit_tag_unification.py](tools/audit_tag_unification.py) — reads the `libs-dev/` jars and reports,
  per unification group, the category tags each equivalent occupies, flagging `[COOKING-INPUT: SKIP]` tags.
  The repeatable basis of [Tag-membership unification](#tag-membership-unification-added-137).

---

## Compat content

Lang keys for JEI info pages: [en_us.json](src/main/resources/assets/delightful_compat/lang/en_us.json),
key pattern `delightful_compat.jei.<group>`.

### Unification groups (`data/delightful_compat/unification/<group>.json`)
| Group | Canonical | Members | Tag | JEI hide |
| --- | --- | --- | --- | --- |
| [sweet_potato](src/main/resources/data/delightful_compat/unification/sweet_potato.json) | `peruviansdelight:camote` | camote, `veggiesdelight:sweet_potato` (+placeholders) | `c:crops/sweet_potato` (+`c:foods/sweet_potato`; merged into `c:foods/vegetable`/`c:crops`/`horse_food`/`villager_plantable_seeds` — see [Tag-membership unification](#tag-membership-unification-added-137)) | **no** (tag-only; see gotcha) |
| [dough](src/main/resources/data/delightful_compat/unification/dough.json) | `farmersdelight:wheat_dough` | wheat_dough, `ramadandelight:small_dough` | `c:foods/dough` | no |
| [milk](src/main/resources/data/delightful_compat/unification/milk.json) | `farmersdelight:milk_bottle` | milk_bottle, `minecraft:milk_bucket` | `c:foods/milk` | no |
| [mashed_potatoes](src/main/resources/data/delightful_compat/unification/mashed_potatoes.json) | `moredelight:mashed_potatoes` | more + slavic + `veggiesdelight:mashed_potatoes` | `c:foods/mashed_potatoes` | yes |
| [pancakes](src/main/resources/data/delightful_compat/unification/pancakes.json) | `oaksdelight:pancakes` | oaks + `slavic_delight:pancakes` | `c:foods/pancakes` | yes |
| [toast](src/main/resources/data/delightful_compat/unification/toast.json) | `oaksdelight:toast` | oaks + `moredelight:toast` + `mynethersdelight:toasts` | `c:foods/toast` | yes |

### Tags (`data/c/tags/item/...`, all MERGE — no `replace`)
[foods/milk](src/main/resources/data/c/tags/item/foods/milk.json),
[drinks/milk](src/main/resources/data/c/tags/item/drinks/milk.json),
[foods/dough](src/main/resources/data/c/tags/item/foods/dough.json),
[crops/sweet_potato](src/main/resources/data/c/tags/item/crops/sweet_potato.json),
[foods/mashed_potatoes](src/main/resources/data/c/tags/item/foods/mashed_potatoes.json),
[foods/pancakes](src/main/resources/data/c/tags/item/foods/pancakes.json).
Tag-membership-unification merges (1.3.7): [crops](src/main/resources/data/c/tags/item/crops.json),
[foods/vegetable](src/main/resources/data/c/tags/item/foods/vegetable.json),
[minecraft horse_food](src/main/resources/data/minecraft/tags/item/horse_food.json),
[minecraft villager_plantable_seeds](src/main/resources/data/minecraft/tags/item/villager_plantable_seeds.json).
1.3.8: [crops/potato](src/main/resources/data/c/tags/item/crops/potato.json) (sweet potato ≡ potato),
[seeds](src/main/resources/data/c/tags/item/seeds.json) (cross-populate `c:seeds` with untagged seeds —
currently `slavic_delight:cucumber_seeds` — fixing compost + breeding for them).

### Conflict + recipes
- Conflict rule: [dough_dupe_loop_ramadan.json](src/main/resources/data/delightful_compat/conflicts/dough_dupe_loop_ramadan.json).
  Rationale: adding `small_dough` to `c:foods/dough` turns Ramadan's `small_dough_from_dough` (1→2) into a
  duplication loop.
- Disable override: [data/ramadandelight/recipe/small_dough_from_dough.json](src/main/resources/data/ramadandelight/recipe/small_dough_from_dough.json)
  — keeps the original ONLY when FD is absent (`neoforge:not mod_loaded farmersdelight`), so the loop recipe is
  dropped whenever unification is in play.
- Replacement: [small_dough_from_wheat_dough_cutting.json](src/main/resources/data/delightful_compat/recipe/small_dough_from_wheat_dough_cutting.json)
  (FD cutting board, 1:1, gated by `item_exists` of both dough items).
- Milk fallback: [cake_from_milk_bucket_fallback.json](src/main/resources/data/delightful_compat/recipe/cake_from_milk_bucket_fallback.json)
  (loads only when `farmersdelight:milk_bottle` does NOT exist).

### Broken-recipe overrides (added 1.2.0)
Pure datapack overrides under `data/<theirmod>/recipe/...` that mirror the upstream recipe **verbatim**
and add a `neoforge:conditions` gate, so the recipe loads unchanged when its dependency exists and drops
silently (no parse error) otherwise. Derived from a real modpack startup log; **not** unification/JEI —
no Java, no `CompatRules` entry, no lang key. Regenerate with `/tmp/gen_overrides.py` (reads the addon
jars; not committed — it's a one-shot dev tool, re-point the jar paths if addons update).

| Addon (count) | Gate | Why it errored upstream |
| --- | --- | --- |
| `farmersknives` (53) | `item_exists` of the output knife | knife recipes shipped with no conditions; the knife only registers when its metal mod is present |
| `oaksdelight` (17) | `item_exists` of the output | `crafting/*_display_case` recipes whose items oaksdelight 1.0.9 never registers (orphaned) |
| `oaksdelight` cleavers (5) | always-false (`item_exists` of a fake id) | `crafting/knives/*_cleaver.json` shipped EMPTY (0-byte → `EOFException`); real recipes live at `cleaver/*`. Override = valid copy, gated off, so no duplicate; cleaver stays craftable via the real recipe |
| `casualnessdelight` (4) | `mod_loaded casualness_delight` + `item_exists` | deep-frying recipes shipped *inside* Peruvian's/More Delight using the `casualness_delight:deep_frying` serializer |
| `brewinandchewin` (1) | `mod_loaded brewinandchewin` + `item_exists` | a fermenting recipe shipped *inside* My Nether's Delight using the `brewinandchewin:fermenting` serializer |

Genuine **fix** (not suppression): [peruviansdelight/recipe/masa_picarones.json](src/main/resources/data/peruviansdelight/recipe/masa_picarones.json)
— upstream uses the deprecated `"item"` result key (1.20 format); 1.21.1 needs `"id"`. Override restores
it corrected. Mods overridden must be ordered `AFTER` in `neoforge.mods.toml` (added `farmersknives`,
`casualness_delight`, `brewinandchewin`); the casualness/brewin originals live in mods we already order after.

### Compostability (added 1.3.5)
[data/neoforge/data_maps/item/compostables.json](src/main/resources/data/neoforge/data_maps/item/compostables.json)
— a **NeoForge data map** (`neoforge:compostables`) that merges (`replace:false`) with NeoForge's builtin to
make plant matter compostable across every addon. Keyed by **common tags**, so it self-guards (an absent mod
just means fewer items) — no `neoforge:conditions` needed and no Java:

| Tag key | chance | mirrors vanilla |
| --- | --- | --- |
| `#c:seeds` | 0.3 (`can_villager_compost:true`) | seeds (the reported gap — cucumber seeds etc.) |
| `#c:crops` | 0.65 | raw crops/veg |
| `#minecraft:saplings` | 0.3 | saplings |
| `#minecraft:leaves` | 0.3 | leaves |

**Root cause** it fixes: in 1.21.1 `ComposterBlock#getValue` reads ONLY the `neoforge:compostables` data map
(verified in the NeoForge sources); the old `ComposterBlock.COMPOSTABLES` Java map is **deprecated and no
longer consulted**. Addons that still register compostability via `COMPOSTABLES.add(...)` silently have no
effect — so their seeds/plants don't compost. Re-declaring via the data map restores them. The file lives
under the **`neoforge` namespace** folder (like contributing to someone else's tag), not `delightful_compat`.
Guarded by `DatapackIntegrityTest#compostablesDataMapUnifiesSeedTag`.

### Animal breeding / feeding (added 1.3.6)
Animals decide breeding/feeding food via `Animal#isFood`, which in 1.21.1 reads a **vanilla item tag**
`minecraft:<animal>_food` (verified: `Chicken#isFood` → `ItemTags.CHICKEN_FOOD`, likewise Pig/Cow/Sheep/
Rabbit/…). Vanilla fills those tags with **hard-coded items**, not common-tag umbrellas — e.g.
`minecraft:chicken_food` lists only the 6 vanilla seeds, so a modded seed (cucumber) can't breed a chicken.
We **merge** the matching `c:` umbrella into each tag (plain datapack tag-merge in the `minecraft` namespace,
`required:false` so an empty/absent umbrella is harmless):

| Animal tag | merges | effect |
| --- | --- | --- |
| `minecraft:chicken_food` | `#c:seeds` | every tagged seed breeds chickens (the reported gap) |
| `minecraft:parrot_food` | `#c:seeds` | every tagged seed tames parrots (parity; parrots don't breed) |
| `minecraft:pig_food` | `#c:foods/vegetable` | modded vegetables breed pigs (vanilla pig diet ⊂ this tag) |

Wheat-eaters (cow/sheep/goat/llama/horse) are left alone — no modded "grain" umbrella and no real gap. Shares
the **same dependency as the compost fix**: an item only benefits if its mod tags it into `c:seeds` /
`c:foods/vegetable`. If a specific seed works for neither composting nor breeding, that mod didn't tag it —
add the id to [c/tags/item/seeds.json](src/main/resources/data/c/tags/item/seeds.json) (fixes both at once;
done for `slavic_delight:cucumber_seeds` in 1.3.8). Guarded by `DatapackIntegrityTest#animalFoodTagsUnifyCategories`.

### Tag-membership unification (added 1.3.7)
"Potato is a potato" for **tag-based** recipes. A duplicate group's items are only interchangeable in a
recipe keyed on tag `T` if **all** of them are in `T`. Addons tag asymmetrically — Veggies richly tags its
`sweet_potato`, Peruvian's barely tags `camote` — so camote was rejected by `farmersdelight:stuffed_pumpkin`
(`c:foods/vegetable`), `horse_food`, etc. Fix = merge the group's unifying tag into each broad category tag
any equivalent occupies, so present-and-future members all join. Pure datapack tag-merge, `required:false`.

Current (sweet potato): [c/tags/item/foods/vegetable.json](src/main/resources/data/c/tags/item/foods/vegetable.json),
[c/tags/item/crops.json](src/main/resources/data/c/tags/item/crops.json),
[c/tags/item/crops/potato.json](src/main/resources/data/c/tags/item/crops/potato.json) (1.3.8 — parity, see below),
[minecraft/tags/item/horse_food.json](src/main/resources/data/minecraft/tags/item/horse_food.json),
[minecraft/tags/item/villager_plantable_seeds.json](src/main/resources/data/minecraft/tags/item/villager_plantable_seeds.json)
— each merges `#c:crops/sweet_potato`. (Pig breeding is already covered: `pig_food` merges `#c:foods/vegetable`,
which now contains the group → camote, resolved transitively.)

**Cooking-safety rule (same as recipe-interchange):** only unify a tag that is NOT a single-input cooking
input (`minecraft:smelting`/`smoking`/`campfire_cooking`/`blasting`, `farmersdelight:cutting`). Expanding such
a tag makes one input match many recipes → ambiguous output. The audit flags these as `[COOKING-INPUT: SKIP]`:
**`c:crops/potato`** (cutting: fries/diced) and **`c:foods/bread`** (cutting: bread_slice).
**Parity exception (1.3.8):** unify a cutting-input tag anyway when an equivalent is ALREADY in it — then the
ambiguity pre-exists and skipping only makes the group inconsistent. `c:crops/potato` already contains
`veggiesdelight:sweet_potato`, so we merge the group there too (camote subs for potato); cutting a camote may
now yield potato_fries/diced rather than camote_cortado — an accepted trade for "sweet potato ≡ potato".
`c:foods/bread` stays skipped (no group equivalent is in it).

Re-run the audit when addons change: **`python tools/audit_tag_unification.py`** (reads the jars in
`libs-dev/`; prints each group's category tags with `[COOKING-INPUT: SKIP]` flags). Other groups currently
need nothing: milk is already unified (we add `milk_bucket` to `c:drinks/milk`), and the rest only overlap on
cooking-unsafe, debuff (`c:foods/food_poisoning`), or rarely-used umbrella tags. Guarded by
`DatapackIntegrityTest#sweetPotatoTagMembershipUnified`.

---

## Editing recipes for common tasks

| I want to… | Touch these (in order) |
| --- | --- |
| **Add a unification group** | `unification/<group>.json` + `data/c/tags/item/<tag>.json` + lang key `delightful_compat.jei.<group>` + add the rule path to `CompatRules#BUNDLED_UNIFICATION` + (optional) ids in `ModIds.Items`/`ModIds.Tags` → then update [Compat content](#compat-content). |
| **Support a new Delight mod** | add an `optional` `[[dependencies]]` block in `neoforge.mods.toml` + `ModIds.Mods` constant + whatever groups/conflicts apply (rows above) + the supported-mods list in this file & README. |
| **Resolve a recipe conflict** | `conflicts/<id>.json` + disable override at `data/<theirmod>/recipe/<recipe>.json` (their content + a failing `neoforge:conditions`) + replacement under `data/delightful_compat/recipe/` + add path to `CompatRules#BUNDLED_CONFLICTS`. |
| **Add a conditional fallback recipe** | new file under `data/delightful_compat/recipe/` with `neoforge:conditions` (`neoforge:not` + `neoforge:item_exists`/`mod_loaded`). |
| **Suppress a broken/orphaned foreign recipe** | copy the upstream recipe verbatim to `data/<theirmod>/recipe/<path>.json`, prepend a `neoforge:conditions` gate (`item_exists` of the output, or `mod_loaded` of the missing serializer's mod) + ensure that mod is ordered `AFTER` in `neoforge.mods.toml` → then update [Broken-recipe overrides](#broken-recipe-overrides-added-120). |
| **Make a duplicate item interchangeable in recipes** | TWO mechanisms. (1) **Tag-based recipes** (the addon recipe already uses `#c:...`): just ensure every equivalent is in that tag — usually merge `#<grouptag>` into the category tag (see [Tag-membership unification](#tag-membership-unification-added-137); run `python tools/audit_tag_unification.py` to find gaps). Pure tag-merge, fixes all such recipes at once. (2) **Item-hardcoded recipes**: override each consuming recipe at `data/<theirmod>/recipe/<path>.json`, swap `{"item":<equiv>}` → `{"tag":<grouptag>}`, self-gate on `mod_loaded:<owning mod>`. Both obey the cooking-safety rule (crafting/cooking-pot only). **Never rewrite a conversion recipe** (result is itself a group member, e.g. `milk_bucket_from_bottles`) and never expand a `[COOKING-INPUT: SKIP]` tag. |
| **Make an item compostable** | add its tag (or id) to the `values` of [data/neoforge/data_maps/item/compostables.json](src/main/resources/data/neoforge/data_maps/item/compostables.json) — prefer a `#c:`/`#minecraft:` tag key (self-guarding) over a raw id; chance mirrors vanilla (seeds 0.3, crops 0.65). NOT a recipe/JEI/Java concern — the 1.21.1 composter reads the `neoforge:compostables` data map only. → then update [Compostability](#compostability-added-135). |
| **Make an item breed/feed an animal** | merge into the vanilla `data/minecraft/tags/item/<animal>_food.json` (the animal's `isFood` reads that tag) — prefer the `c:` umbrella (`#c:seeds`, `#c:foods/vegetable`) with `required:false`. NOT Java. → then update [Animal breeding / feeding](#animal-breeding--feeding-added-136). |
| **Add/rename a config option** | `DelightfulCompatConfig` field + its reader (`CompatValidator` and/or the JEI plugin) + the config table in README. |
| **Change a JEI behavior** | `DelightfulCompatJeiPlugin` (`#registerRecipes` / `#onRuntimeAvailable`). |

---

## Invariants & gotchas

- **1.21.1 datapack folders are singular**: `recipe/`, `tags/item/`. Pack format **48**.
- **Conditions are the safety mechanism**: wrap every patch in `neoforge:conditions`. Multiple entries in the
  array are AND-ed. Negate with `{"type":"neoforge:not","value":{…}}`. `and`/`or` use `"values":[…]`.
  To disable a foreign recipe: override its file and give it conditions that **fail** in your scenario.
- **Tags merge by default** (no `replace`). Use entry objects `{"id":"…","required":false}` so a missing
  item from an absent mod never errors. `__comment` keys are ignored by the codecs (safe in tags & recipes).
- **`CompatRules#BUNDLED_UNIFICATION` / `#BUNDLED_CONFLICTS` must mirror the JSON files** under
  `data/delightful_compat/{unification,conflicts}/`. They seed defaults for JEI before a world loads; the
  datapack reload (`CompatRuleManager`) is authoritative at runtime, but if you add a rule file and forget the
  array, JEI won't see it pre-world. **Always update both.**
- **JEI is compile-only** (`compileOnly` API in `build.gradle`); never add a hard runtime dependency. The
  plugin class must stay self-contained so the mod loads without JEI.
- **Override wins by load order**: our datapack overrides a foreign recipe because we declare `ordering="AFTER"`
  that mod in `neoforge.mods.toml`. Keep new addon deps ordered AFTER.
- **Recipe result format is `{"id":…,"count":…}`** in 1.21.1 (not `"item":`). Some addons still ship the old
  `"item":` form and log their own parse errors — those are the addon's bug, not ours.
- **Real ids, not guesses**: the shipped ids match FD 1.3.2 / Oaks 1.0.9 / Ramadan 1.7 / Peruvian 1.3.0 /
  More 26.05.20a / Slavic 0.3.2. Re-verify from the jar if bumping an addon. Note `slavic_delight` has an
  underscore; the others don't.
- **Spotless auto-formats on build** (`compileJava dependsOn spotlessApply`, palantir 4-space/120-col). Don't
  hand-fight the formatter; run `./gradlew spotlessApply`. New `.java` files should keep the Apache header
  comment (the existing convention; Spotless does not insert it).
- **Two test guards enforce the cross-file invariants** — if you add a group/conflict and a test fails:
  `CompatRulesTest` ⇒ you forgot `CompatRules.BUNDLED_*`; `DatapackIntegrityTest` ⇒ you forgot the lang key,
  the `c:` tag file, or a replacement recipe. Fix the content, not the test.
- **JEI hiding requires the canonical to exist.** `DelightfulCompatJeiPlugin#onRuntimeAvailable` skips a
  group whose `canonical` item isn't registered, so it never hides a group's last visible item when the
  canonical's mod is absent (e.g. mashed potatoes with More Delight missing). Keep this guard.
- **Ingredient interchange is ONLY safe on crafting-table recipes** (`crafting_shaped`/`shapeless`).
  Single-input station recipes — `minecraft:smelting`/`smoking`/`campfire_cooking`/`blasting` and
  `farmersdelight:cutting` — are one-in-one-out: rewriting their input to a `c:` tag makes one input
  match many recipes, so only one output wins. 1.3.0 rewrote `bread_from_smelting`/`flat_bread_from_*`
  to `c:foods/dough` and every dough then smelted ambiguously to bread/flat_bread/pancakes (15
  conflicts). 1.3.2 restricts interchange to crafting types ([/tmp/gen_unify.py] `CRAFTING` set).
  **Output canonicalization (collapse) is fine on any type** — it changes the result, never adds a
  matching recipe, so it can't conflict. Re-audit conflicts with `/tmp/analyze3.py` (dedupes by recipe
  id so the override wins, then flags same-type same-input multi-output).
- **Sweet potato uses tag-membership unification, not recipe-interchange/collapse.** Its two real items
  (`peruviansdelight:camote`, `veggiesdelight:sweet_potato`) have distinct *cooked* forms, so rewriting their
  cooking recipes or collapsing outputs is wrong (kept out of the `gen_unify` recipe pass). BUT they must
  still be mutually usable as *ingredients* ("potato is a potato"). The gap (1.3.7): Veggies tags ITS sweet
  potato into broad category tags (`c:foods/vegetable`, `c:crops`, `minecraft:horse_food`,
  `villager_plantable_seeds`) but Peruvian's barely tags camote at all — so e.g. `farmersdelight`'s
  `stuffed_pumpkin_block` (keyed on `c:foods/vegetable`) rejected camote. Fix = merge `#c:crops/sweet_potato`
  into those category tags (see [Tag-membership unification](#tag-membership-unification-added-137)).
  **`c:crops/potato` IS now unified too (1.3.8)** so camote subs for potato like Veggies' sweet_potato does —
  even though it's a cutting input — because Veggies' sweet_potato already lives there (the cutting overlap
  pre-exists) and the user wants sweet potato ≡ potato. See the cooking-safety parity exception below.
- **`oaksdelight:butter` has no upstream recipe** (its `cooking/butter.json` is a mislabeled muffins
  dup); we ship a Cooking Pot butter recipe at that path. If oaksdelight fixes it, drop our override.
- **A broken-recipe override must self-gate.** Because the override file ships in OUR jar, it is loaded
  even when the target addon is absent — so without a condition it would re-introduce the very recipe (and
  error) we're suppressing. The `item_exists`/`mod_loaded` gate drops it cleanly in the addon-absent case
  too. The override must also be ordered `AFTER` the addon (and after whatever mod *ships* the recipe, for
  the casualness/brewin ones nested inside Peruvian's/More/My Nether's) or it won't win load order.
- **A group's `tag` field is the primary unifying tag, but a group may rely on extra tags** (e.g.
  sweet_potato also populates `c:foods/sweet_potato`). `DatapackIntegrityTest` only checks the `tag` field's
  file exists; extra tag files are validated by the generic "all data JSON parses" walk.
- **Animal food is a tag, not code, in 1.21.1.** `Animal#isFood` reads `minecraft:<animal>_food` (e.g.
  `Chicken#isFood` → `ItemTags.CHICKEN_FOOD`), so make a food breed/feed an animal by **merging** into
  `data/minecraft/tags/item/<animal>_food.json` — never Java. Vanilla fills these with hard-coded items (not
  `c:` umbrellas), which is why modded seeds don't breed chickens. Merge the `c:` umbrella with
  `required:false`. The vanilla tag files live in the **client-extra** jar, not the merged/sources jar (look
  there to see vanilla contents).
- **Compostability is a data map, not code, in 1.21.1.** `ComposterBlock#getValue` reads
  `NeoForgeDataMaps.COMPOSTABLES`; the deprecated `ComposterBlock.COMPOSTABLES` map is no longer consulted, so
  do NOT try to fix compost gaps in Java — ship/extend `data/neoforge/data_maps/item/compostables.json`. Keep
  keys as **common tags** (`#c:seeds`, …) so the file needs no conditions (empty tags resolve to nothing). Our
  file merges with NeoForge's builtin and, ordered AFTER the addons, also overrides any wrong values — fine for
  seeds (universally 0.3). Data map value codec is **strict**: no `__comment` keys in that file (unlike tags/
  recipes). Field names are snake_case: `chance`, `can_villager_compost`.

---

## Build & verify

Versions in [gradle.properties](gradle.properties) + [build.gradle](build.gradle): MC `1.21.1`, NeoForge
`21.1.233`, JEI `19.27.0.340`, Java 21, Gradle 8.10, ModDevGradle `2.0.141`, Spotless `6.25.0`
(palantir-java-format), JUnit `5.10.2`. License **Apache-2.0** ([LICENSE](LICENSE)). `JAVA_HOME` must point at
a JDK 21 (e.g. `C:\Program Files\Java\jdk-21`).

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-21"
./gradlew build            # spotlessApply -> spotlessCheck -> compile -> JUnit (unitTest harness) -> jar
./gradlew test             # JUnit only
./gradlew spotlessApply    # auto-format ("prettier for Java"); runs automatically before compileJava
./gradlew runServer        # dev server; loads any jars in libs-dev/ as mods (datapack validation)
./gradlew runClient        # for JEI hiding / info-page checks
```

CI: [.github/workflows/build.yml](.github/workflows/build.yml) runs `./gradlew build` on JDK 21 per push/PR
to `main`. Release: manual `gh release create vX.Y.Z build/libs/*.jar` after bumping `gradle.properties` +
`CHANGELOG.md`.

**Smoke test signal** (server log): `[DelightfulCompat] N unification group(s) loaded`, the validator's
per-group "X/Y equivalent item(s) present" lines, and **no** error mentioning `delightful_compat`. A missing
canonical logs a WARN and continues (this is correct graceful degradation).
