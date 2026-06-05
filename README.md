# Delightful Compat

A **compatibility / unification** mod for **Farmer's Delight** and its *Delight* addons
(Oaks, Ramadan, Peruvian, More, Slavic, …) on **NeoForge 1.21.1**.

It adds almost no content of its own. Instead it makes a stack of Delight addons play nicely
together in big modpacks by:

- **Unifying equivalent ingredients** through common (`c:`) item tags, so a recipe accepts an
  ingredient no matter which Delight mod added it.
- **Picking a canonical item** per duplicate group, so newly-added/replacement recipes output a
  single standard item instead of cluttering the game with near-identical duplicates.
- **Resolving recipe conflicts** — disabling the colliding recipe and re-adding the lost output,
  preferring to move one recipe onto a Farmer's Delight station (cutting board, cooking pot, …)
  rather than deleting content.
- **Providing milk-bucket fallbacks** when a "milk bottle" item is missing.
- **Cleaning up JEI** — hiding non-canonical duplicates and adding info pages that explain the
  unification.

Everything is **optional and crash-safe**: every patch is guarded by `neoforge:conditions`, and
every dependency is declared optional, so a missing Delight mod simply means the relevant patch is
skipped.

---

## Supported mods (all six, verified by extracting the real NeoForge 1.21.1 jars)

| Mod | Mod id | How it's covered |
| --- | --- | --- |
| Farmer's Delight | `farmersdelight` | Central parent — milk + dough unification, canonical milk bottle / wheat dough. |
| Oaks Delight | `oaksdelight` | Its `c:foods/milk` / `c:foods/dough` pancake recipes are fixed by the milk + dough unification; pancakes duplicate unified. |
| Ramadan Delight | `ramadandelight` | `small_dough` unified into `c:foods/dough`; its dupe-loop recipe resolved onto the cutting board. |
| Peruvian's Delight | `peruviansdelight` | `camote` is the canonical sweet potato. |
| More Delight | `moredelight` | `mashed_potatoes` unified with Slavic's; bread-slice/knife tags already merge. |
| Slavic Delight | `slavic_delight` | `mashed_potatoes` + `pancakes` unified with More/Oaks. |

All six are declared as **optional** dependencies, so any subset can be installed.

## What it does out of the box

| Area | Real problem found | Fix shipped |
| --- | --- | --- |
| **Milk** | Oaks pancakes (and others) read `c:foods/milk`, but Farmer's Delight only fills `c:drinks/milk`, and **neither** tag contains `minecraft:milk_bucket`. So milk recipes fail with just a bucket. | Add `milk_bottle` + `milk_bucket` to `c:foods/milk` and `milk_bucket` to `c:drinks/milk`. Plus a conditional cake-from-bucket recipe when no milk bottle exists. |
| **Dough** | `ramadandelight:small_dough` is **not** in `c:foods/dough` (only FD's `wheat_dough` is), so Ramadan dough can't be used in Oaks/FD dough recipes. | Add `small_dough` to `c:foods/dough`. |
| **Conflict** | Once `small_dough` is in `c:foods/dough`, Ramadan's `small_dough_from_dough` (`1× dough → 2× small_dough`) becomes a duplication loop. | Disable that recipe when FD is present; re-add a 1:1 **cutting-board** recipe so `small_dough` is still obtainable. |
| **Sweet potato** | Only Peruvian's Delight ships one (`camote`); no unifying tag exists. | Create `c:crops/sweet_potato` with `camote` canonical + placeholders for other mods' sweet potatoes. |
| **Mashed potatoes** | **Both** More Delight and Slavic Delight add a "Mashed Potatoes" item — a true duplicate. | Unify under `c:foods/mashed_potatoes`, canonical = More's; hide Slavic's in JEI. |
| **Pancakes** | **Both** Oaks Delight and Slavic Delight add a "Pancakes" item — a true duplicate. | Unify under `c:foods/pancakes`, canonical = Oaks'; hide Slavic's in JEI. |

---

## Architecture (3 layers)

1. **Datapack layer** (`src/main/resources/data/…`) — the static JSON that does the real work
   (tags, recipe-disable overrides, replacement/fallback recipes). Every patch is wrapped in
   `neoforge:conditions`, so it only applies when the relevant mods/items are present.
2. **Config layer** (`DelightfulCompatConfig`) — TOML toggles at `config/delightful_compat-common.toml`.
3. **Rule + Java layer** — data-driven rule files (`unification/*.json`, `conflicts/*.json`) read by
   Java to drive JEI hiding/info-pages and to validate that configured canonical items exist
   (clear log warnings otherwise).

---

## Config options (`config/delightful_compat-common.toml`)

| Option | Default | Effect |
| --- | --- | --- |
| `enableRecipePatches` | `true` | Master switch for conflict resolution (honored by JEI/validation; datapack JSON is also mod/item-gated). |
| `enableIngredientUnification` | `true` | Master switch for ingredient unification. |
| `enableMilkBucketFallbacks` | `true` | Allow `minecraft:milk_bucket` to satisfy milk ingredients. |
| `enableJeiCleanup` | `true` | Master switch for all JEI tweaks. |
| `hideDuplicateItems` | `true` | Hide non-canonical duplicate items from JEI. |
| `hideDisabledRecipes` | `true` | Hint to omit disabled recipes from JEI (they're already removed at the datapack layer). |
| `showCompatibilityNotes` | `true` | Add a JEI info page to each canonical item. |
| `canonicalSweetPotatoItem` | `peruviansdelight:camote` | Canonical sweet potato. |
| `canonicalSmallDoughItem` | `ramadandelight:small_dough` | Canonical small dough. |
| `canonicalLargeDoughItem` | `farmersdelight:wheat_dough` | Canonical standard dough. |

If a configured canonical item does not exist, a **clear WARN** is logged and the rule's own
default canonical is used — never a crash.

---

## How to add a new Delight mod / unification group

No Java recompile is needed for tags, recipes, or rules — they're pure datapack JSON and are
**override-able by any datapack** (drop files in your pack's `data/<ns>/unification/`, etc.).

1. **Add ids** to [`ModIds.java`](src/main/java/com/delightfulcompat/ModIds.java) (optional, for clarity).
2. **Declare the group**: add `data/delightful_compat/unification/<group>.json`:
   ```json
   {
     "group": "tomato",
     "canonical": "farmersdelight:tomato",
     "equivalents": ["farmersdelight:tomato", "othermod:tomato"],
     "tag": "c:foods/tomato",
     "hideNonCanonicalInJei": true
   }
   ```
   (If you ship a brand-new tag, also add `data/c/tags/item/…json` listing the members with
   `"required": false`.) Then add a matching lang key `delightful_compat.jei.<group>` for the JEI
   info page.
3. **If a recipe collides**: add `data/delightful_compat/conflicts/<id>.json`, an override that
   disables the original (copy the original recipe and add a failing `neoforge:conditions`), and a
   replacement recipe under `data/delightful_compat/recipe/` (prefer an FD station).
4. If you added a brand-new bundled rule file and want it available to JEI *before* a world loads,
   also list it in `BUNDLED_UNIFICATION` / `BUNDLED_CONFLICTS` in
   [`CompatRules.java`](src/main/java/com/delightfulcompat/compat/CompatRules.java). (Datapack reload
   is authoritative regardless.)

### The NeoForge conditions you'll use
- Disable a foreign recipe: override its file and add a condition that **fails** in your scenario,
  e.g. `{"type":"neoforge:not","value":{"type":"neoforge:mod_loaded","modid":"farmersdelight"}}`.
- Gate a replacement/fallback: `{"type":"neoforge:item_exists","item":"mod:item"}` (multiple
  conditions in the array are AND-ed) or `{"type":"neoforge:not","value":{…}}` for "only if absent".

---

## Build

```bash
./gradlew build      # produces build/libs/delightful-compat-1.0.0.jar
```

Platform: NeoForge `21.1.233`, Minecraft `1.21.1`, Java 21. JEI (`19.27.0.340`) is an **optional**
compile-time dependency only.

## Manual verification matrix

Run `./gradlew runClient` and drop addon jars into the run `mods/` folder:

1. **FD only** → mod loads, no errors, all conditional patches correctly skip.
2. **FD + one addon** → unified ingredients work; duplicates hidden in JEI; info page shows.
3. **FD + all addons** → dough conflict resolved (cutting-board small dough works, the dupe-loop
   recipe is gone from JEI); sweet-potato/milk recipes accept all variants.
4. **Remove the milk-bottle provider** → the milk-bucket cake fallback recipe appears and crafts.
5. **Set a bogus `canonical*` config id** → a clear WARN is logged, no crash.

See `IMPLEMENTATION_NOTES.md` for the full deliverables summary and limitations.

## Development

```bash
./gradlew build          # spotlessApply (format) -> spotlessCheck -> compile -> JUnit tests -> jar
./gradlew test           # JUnit only (NeoForge unit-test harness)
./gradlew spotlessApply  # auto-format (palantir-java-format, "prettier for Java")
./gradlew spotlessCheck  # format gate (also part of build)
```

CI (`.github/workflows/build.yml`) runs `./gradlew build` on JDK 21 for every push/PR to `main` and
uploads the jar artifact.

## License

[Apache License 2.0](LICENSE).
