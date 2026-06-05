# Delightful Compat — Implementation Notes

NeoForge **1.21.1** (`21.1.233`), Java 21, ModDevGradle `2.0.42-beta`. Mod id `delightful_compat`.
Status: **compiles** (`./gradlew build` → `build/libs/delightful-compat-1.0.0.jar`) and **boots clean on a
dedicated server** with Farmer's Delight + Oaks + Ramadan + Peruvian + Slavic (5 unification groups load,
the dupe-loop recipe is disabled, zero Delightful-Compat datapack errors).

## Files created

### Build / metadata
- `settings.gradle`, `gradle.properties`, `build.gradle` (NeoForge + optional JEI `19.27.0.340`), `.gitignore`
- `gradle/wrapper/*`, `gradlew`, `gradlew.bat` (copied from the proven `minecraft-manager/agent-neoforge`)
- `src/main/resources/META-INF/neoforge.mods.toml` — all 6 Delight mods + JEI as **optional** deps
- `src/main/resources/pack.mcmeta` — pack_format 48

### Java (`src/main/java/com/delightfulcompat/`)
- `DelightfulCompat.java` — `@Mod` entry: registers config, seeds bundled rules, hooks datapack reload + validation
- `DelightfulCompatConfig.java` — the TOML config (all options below)
- `ModIds.java` — **single source** of every mod-id / item-id / tag constant (the place to edit for new mods)
- `compat/UnificationGroup.java`, `compat/RecipeConflict.java` — data-driven rule records (+ Codecs)
- `compat/CompatRules.java` — in-memory rule store + bundled-default loader (so JEI has data at startup)
- `compat/CompatRuleManager.java` — datapack reload listener (`unification/` + `conflicts/`), pack-overridable
- `compat/CompatValidator.java` — startup item-registry checks, clear WARN + graceful fallback
- `integration/jei/DelightfulCompatJeiPlugin.java` — optional JEI: hide duplicates + info pages

### Datapack (`src/main/resources/data/`)
- Common tags (merge with existing): `c/tags/item/foods/milk.json`, `c/tags/item/drinks/milk.json`,
  `c/tags/item/foods/dough.json`, `c/tags/item/crops/sweet_potato.json`,
  `c/tags/item/foods/mashed_potatoes.json`, `c/tags/item/foods/pancakes.json`
- Rule files (source of truth, also bundled defaults): `delightful_compat/unification/{sweet_potato,dough,milk,mashed_potatoes,pancakes}.json`,
  `delightful_compat/conflicts/dough_dupe_loop_ramadan.json`
- Recipe disable-override: `data/ramadandelight/recipe/small_dough_from_dough.json` (drops the dupe loop when FD present)
- Replacement recipe: `delightful_compat/recipe/small_dough_from_wheat_dough_cutting.json` (FD cutting board)
- Conditional fallback: `delightful_compat/recipe/cake_from_milk_bucket_fallback.json` (only when no milk bottle exists)

### Assets / docs
- `assets/delightful_compat/lang/en_us.json` — JEI info-page text for each group
- `README.md`, `IMPLEMENTATION_NOTES.md` (this file)

## New config options (`config/delightful_compat-common.toml`)
`enableRecipePatches`, `enableIngredientUnification`, `enableMilkBucketFallbacks`, `enableJeiCleanup`,
`hideDuplicateItems`, `hideDisabledRecipes`, `showCompatibilityNotes`, `canonicalSweetPotatoItem`,
`canonicalSmallDoughItem`, `canonicalLargeDoughItem`.

## Compatibility rules added (5 unification groups + 1 conflict)
| Group / conflict | Canonical | Members | JEI hide? |
| --- | --- | --- | --- |
| sweet_potato | `peruviansdelight:camote` | camote (+ placeholders) | yes |
| dough | `farmersdelight:wheat_dough` | wheat_dough, `ramadandelight:small_dough` | no |
| milk | `farmersdelight:milk_bottle` | milk_bottle, `minecraft:milk_bucket` | no |
| mashed_potatoes | `moredelight:mashed_potatoes` | more + `slavic_delight:mashed_potatoes` | yes |
| pancakes | `oaksdelight:pancakes` | oaks + `slavic_delight:pancakes` | yes |
| conflict: dough_dupe_loop_ramadan | — | disables `ramadandelight:small_dough_from_dough`, adds cutting-board replacement | — |

## How to add a future unification group / addon
See README → "How to add a new Delight mod". In short: add ids to `ModIds.java`, drop a
`unification/<group>.json` rule + a `c:` tag, add a `delightful_compat.jei.<group>` lang line, and (for
conflicts) an override + replacement recipe. No Java recompile is needed for tags/recipes/rules; datapacks
can override everything.

## Limitations / follow-up
- **Item ids were extracted from the real jars**, so the shipped rules match the current versions
  (FD 1.3.2, Oaks 1.0.9, Ramadan 1.7, Peruvian 1.3.0, More 26.05.20a, Slavic 0.3.2). If an addon renames an
  item in a future version, update `ModIds.java` + the relevant JSON.
- **More Delight** could not be loaded in the runtime smoke test because it hard-requires `delightlib`
  (not on Modrinth). Its rule/tag JSON is structurally identical to the runtime-validated ones and is covered
  by the build; the validator logs a clear WARN if `moredelight:mashed_potatoes` is absent (verified live).
- `enableRecipePatches` / `enableMilkBucketFallbacks` are honored at the Java/JEI/validation layer; the
  datapack JSON itself is gated on mod/item *presence* via `neoforge:conditions` (NeoForge has no built-in
  "config value" recipe condition). Pack authors can disable any bundled patch via a datapack override.
  Runtime config-gated recipe removal is a possible future enhancement (custom condition / recipe-manager hook).
- `libs-dev/` holds local copies of the addon jars only for `runClient`/`runServer` testing; it is not part
  of the published jar and should not be committed (the real jars are copyrighted — download from Modrinth/CurseForge).
- Forge/Fabric are out of scope — NeoForge only.

## Manual verification (matrix)
`./gradlew runServer` (or `runClient`) with addon jars in `libs-dev/`:
1. FD only → loads, all conditional patches skip. ✅ (validated)
2. FD + Oaks + Ramadan + Peruvian + Slavic → 5 groups load, dough conflict resolved, no errors. ✅ (validated)
3. Remove milk-bottle provider → cake-from-bucket fallback appears.
4. Bogus `canonical*` config id → clear WARN, no crash. ✅ (behavior validated via the More-absent WARN)
