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
