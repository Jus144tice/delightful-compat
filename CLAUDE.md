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
- **JEI cleanup** — hide non-canonical duplicates, add info pages.

Everything is optional and crash-safe: every datapack patch is guarded by `neoforge:conditions`, and every
mod dependency is `optional`. A missing Delight mod just means the relevant patch is skipped.

**Supported mods** (rules verified by extracting the real jars): `farmersdelight`, `oaksdelight`,
`ramadandelight`, `peruviansdelight`, `moredelight`, `slavic_delight`. JEI is an optional, compile-only
integration.

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
  `optional` deps.
- [pack.mcmeta](src/main/resources/pack.mcmeta) — `pack_format` 48.
- [build.gradle](build.gradle) — NeoForge block, optional JEI dep, the guarded `libs-dev/` dev-mod loader.
- [gradle.properties](gradle.properties) — all versions (MC, NeoForge, JEI, mod).
- [libs-dev/](libs-dev/) — drop addon jars here for `runClient`/`runServer` (never committed; see its README.txt).

---

## Compat content

Lang keys for JEI info pages: [en_us.json](src/main/resources/assets/delightful_compat/lang/en_us.json),
key pattern `delightful_compat.jei.<group>`.

### Unification groups (`data/delightful_compat/unification/<group>.json`)
| Group | Canonical | Members | Tag | JEI hide |
| --- | --- | --- | --- | --- |
| [sweet_potato](src/main/resources/data/delightful_compat/unification/sweet_potato.json) | `peruviansdelight:camote` | camote (+placeholders) | `c:crops/sweet_potato` | yes |
| [dough](src/main/resources/data/delightful_compat/unification/dough.json) | `farmersdelight:wheat_dough` | wheat_dough, `ramadandelight:small_dough` | `c:foods/dough` | no |
| [milk](src/main/resources/data/delightful_compat/unification/milk.json) | `farmersdelight:milk_bottle` | milk_bottle, `minecraft:milk_bucket` | `c:foods/milk` | no |
| [mashed_potatoes](src/main/resources/data/delightful_compat/unification/mashed_potatoes.json) | `moredelight:mashed_potatoes` | more + `slavic_delight:mashed_potatoes` | `c:foods/mashed_potatoes` | yes |
| [pancakes](src/main/resources/data/delightful_compat/unification/pancakes.json) | `oaksdelight:pancakes` | oaks + `slavic_delight:pancakes` | `c:foods/pancakes` | yes |

### Tags (`data/c/tags/item/...`, all MERGE — no `replace`)
[foods/milk](src/main/resources/data/c/tags/item/foods/milk.json),
[drinks/milk](src/main/resources/data/c/tags/item/drinks/milk.json),
[foods/dough](src/main/resources/data/c/tags/item/foods/dough.json),
[crops/sweet_potato](src/main/resources/data/c/tags/item/crops/sweet_potato.json),
[foods/mashed_potatoes](src/main/resources/data/c/tags/item/foods/mashed_potatoes.json),
[foods/pancakes](src/main/resources/data/c/tags/item/foods/pancakes.json).

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

---

## Editing recipes for common tasks

| I want to… | Touch these (in order) |
| --- | --- |
| **Add a unification group** | `unification/<group>.json` + `data/c/tags/item/<tag>.json` + lang key `delightful_compat.jei.<group>` + add the rule path to `CompatRules#BUNDLED_UNIFICATION` + (optional) ids in `ModIds.Items`/`ModIds.Tags` → then update [Compat content](#compat-content). |
| **Support a new Delight mod** | add an `optional` `[[dependencies]]` block in `neoforge.mods.toml` + `ModIds.Mods` constant + whatever groups/conflicts apply (rows above) + the supported-mods list in this file & README. |
| **Resolve a recipe conflict** | `conflicts/<id>.json` + disable override at `data/<theirmod>/recipe/<recipe>.json` (their content + a failing `neoforge:conditions`) + replacement under `data/delightful_compat/recipe/` + add path to `CompatRules#BUNDLED_CONFLICTS`. |
| **Add a conditional fallback recipe** | new file under `data/delightful_compat/recipe/` with `neoforge:conditions` (`neoforge:not` + `neoforge:item_exists`/`mod_loaded`). |
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
