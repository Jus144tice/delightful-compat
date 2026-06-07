/*
 * Copyright 2026 Delightful Compat contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.delightfulcompat;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Pure (no-Minecraft) checks over the shipped datapack JSON, catching cross-file drift that the
 * Codec tests can't see — every unification group must have a JEI lang key and an existing {@code c:}
 * tag file, every conflict's replacement recipes must exist as files, and all data JSON must parse.
 * These mirror the "Editing recipes for common tasks" checklist in CLAUDE.md.
 */
class DatapackIntegrityTest {

    private static final Gson GSON = new Gson();

    @Test
    void everyUnificationGroupHasLangKeyAndTagFile() throws Exception {
        JsonObject lang = readJson("/assets/delightful_compat/lang/en_us.json").getAsJsonObject();
        for (Path rulePath : listJson("/data/delightful_compat/unification")) {
            JsonObject rule = parse(rulePath);
            String group = rule.get("group").getAsString();
            assertTrue(rule.has("canonical"), group + ": rule must declare a canonical");

            assertTrue(
                    lang.has("delightful_compat.jei." + group),
                    "missing lang key 'delightful_compat.jei." + group + "' for group " + group);

            String tag = rule.get("tag").getAsString(); // e.g. "c:foods/milk"
            String[] ns = tag.split(":", 2);
            String tagResource = "/data/" + ns[0] + "/tags/item/" + ns[1] + ".json";
            assertNotNull(resource(tagResource), "missing tag file " + tagResource + " for group " + group);
        }
    }

    @Test
    void everyConflictReplacementRecipeExists() throws Exception {
        for (Path conflictPath : listJson("/data/delightful_compat/conflicts")) {
            JsonObject conflict = parse(conflictPath);
            for (JsonElement r : conflict.getAsJsonArray("replacementRecipes")) {
                String[] ns = r.getAsString().split(":", 2);
                String recipeResource = "/data/" + ns[0] + "/recipe/" + ns[1] + ".json";
                assertNotNull(resource(recipeResource), "missing replacement recipe " + recipeResource);
            }
        }
    }

    @Test
    void compostablesDataMapUnifiesSeedTag() throws Exception {
        // The compost fix is a NeoForge data map merged with the builtin (keyed by common plant tags),
        // so every tagged seed/plant across the Delight addons composts even when an addon only used the
        // deprecated ComposterBlock.COMPOSTABLES Java API (which the 1.21.1 composter no longer reads).
        JsonObject map =
                readJson("/data/neoforge/data_maps/item/compostables.json").getAsJsonObject();
        JsonObject values = map.getAsJsonObject("values");
        assertNotNull(values, "compostables data map must have a 'values' object");
        assertTrue(values.has("#c:seeds"), "compostables must cover the #c:seeds tag (the reported gap)");
        assertTrue(
                values.getAsJsonObject("#c:seeds").get("chance").getAsDouble() == 0.3,
                "seeds should compost at the vanilla 0.3 chance");
    }

    @Test
    void animalFoodTagsUnifyCategories() throws Exception {
        // Animals read minecraft:<animal>_food tags (Chicken/Pig/Parrot#isFood). Vanilla lists hard-coded
        // items, so modded seeds/veg of the same category don't breed them. We merge the common umbrella
        // tag so e.g. every seed breeds chickens (the reported cucumber-seeds gap).
        assertTagMerges("chicken_food", "#c:seeds");
        assertTagMerges("parrot_food", "#c:seeds");
        assertTagMerges("pig_food", "#c:foods/vegetable");
    }

    private static void assertTagMerges(String tag, String expectedEntry) throws Exception {
        assertTagMergesAt("/data/minecraft/tags/item/" + tag + ".json", expectedEntry);
    }

    private static void assertTagMergesAt(String resourcePath, String expectedEntry) throws Exception {
        JsonObject t = readJson(resourcePath).getAsJsonObject();
        boolean found = false;
        for (JsonElement v : t.getAsJsonArray("values")) {
            String id = v.isJsonObject() ? v.getAsJsonObject().get("id").getAsString() : v.getAsString();
            if (expectedEntry.equals(id)) {
                found = true;
            }
        }
        assertTrue(found, resourcePath + " must merge " + expectedEntry);
    }

    @Test
    void sweetPotatoTagMembershipUnified() throws Exception {
        // "Potato is a potato": every grouped sweet potato must share the category tags that any one of
        // them is in, so e.g. Peruvian's camote works in the stuffed-pumpkin recipe (keyed on
        // c:foods/vegetable) just like Veggies' sweet_potato. We merge the group tag into the cooking-SAFE
        // category tags (NOT c:crops/potato, which is a cutting input -> would make camote ambiguous).
        assertTagMergesAt("/data/c/tags/item/foods/vegetable.json", "#c:crops/sweet_potato");
        assertTagMergesAt("/data/c/tags/item/crops.json", "#c:crops/sweet_potato");
        assertTagMergesAt("/data/minecraft/tags/item/horse_food.json", "#c:crops/sweet_potato");
        assertTagMergesAt("/data/minecraft/tags/item/villager_plantable_seeds.json", "#c:crops/sweet_potato");
        // Sweet potato also subs for potato (c:crops/potato), matching Veggies' sweet_potato which lives there.
        assertTagMergesAt("/data/c/tags/item/crops/potato.json", "#c:crops/sweet_potato");
    }

    @Test
    void untaggedSeedsCrossPopulatedIntoCSeeds() throws Exception {
        // Slavic Delight's cucumber_seeds ships in NO tags, so c:seeds-keyed mechanics (our compost data map
        // and chicken/parrot breeding merge) missed it. We add it to c:seeds, fixing compost + breeding at once.
        assertTagMergesAt("/data/c/tags/item/seeds.json", "slavic_delight:cucumber_seeds");
    }

    @Test
    void allShippedDataJsonParses() throws Exception {
        Path dataRoot = Path.of(resource("/data").toURI());
        try (Stream<Path> walk = Files.walk(dataRoot)) {
            walk.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                try (var reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                    GSON.fromJson(reader, JsonElement.class);
                } catch (Exception e) {
                    fail("invalid JSON: " + p + " -> " + e.getMessage());
                }
            });
        }
    }

    // ---- helpers -------------------------------------------------------------

    private static URL resource(String path) {
        return DatapackIntegrityTest.class.getResource(path);
    }

    private static JsonElement readJson(String classpath) throws Exception {
        try (InputStream in = DatapackIntegrityTest.class.getResourceAsStream(classpath)) {
            assertNotNull(in, "resource not found: " + classpath);
            return GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
        }
    }

    private static JsonObject parse(Path p) throws Exception {
        try (var reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, JsonObject.class);
        }
    }

    private static List<Path> listJson(String dirClasspath) throws Exception {
        URL url = resource(dirClasspath);
        assertNotNull(url, "directory not found on classpath: " + dirClasspath);
        List<Path> out = new ArrayList<>();
        try (Stream<Path> list = Files.list(Path.of(url.toURI()))) {
            list.filter(p -> p.toString().endsWith(".json")).forEach(out::add);
        }
        assertTrue(!out.isEmpty(), "no rule files found under " + dirClasspath);
        return out;
    }
}
