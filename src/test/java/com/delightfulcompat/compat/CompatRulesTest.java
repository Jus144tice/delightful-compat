/*
 * Copyright 2026 Delightful Compat contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package com.delightfulcompat.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Exercises the real {@link UnificationGroup}/{@link RecipeConflict} Codecs against the JSON bundled
 * in the jar, via {@link CompatRules#loadBundledDefaults()}. This locks in the invariant from
 * CLAUDE.md that {@code CompatRules.BUNDLED_*} stays in sync with the rule files on disk.
 */
class CompatRulesTest {

    @BeforeAll
    static void load() {
        CompatRules.loadBundledDefaults();
    }

    @Test
    void allBundledGroupsParse() {
        Set<String> names =
                CompatRules.groups().stream().map(UnificationGroup::group).collect(Collectors.toSet());
        assertEquals(Set.of("sweet_potato", "dough", "milk", "mashed_potatoes", "pancakes"), names);
    }

    @Test
    void oneConflictRuleParses() {
        assertEquals(1, CompatRules.conflicts().size());
        assertEquals("dough_dupe_loop_ramadan", CompatRules.conflicts().get(0).id());
    }

    @Test
    void everyCanonicalIsAmongItsEquivalents() {
        for (UnificationGroup g : CompatRules.groups()) {
            assertTrue(
                    g.equivalents().contains(g.canonical()),
                    "canonical of group '" + g.group() + "' must be listed in its equivalents");
        }
    }

    @Test
    void everyGroupDeclaresAUnifyingTag() {
        for (UnificationGroup g : CompatRules.groups()) {
            assertTrue(g.tag().isPresent(), "group '" + g.group() + "' should declare a tag");
        }
    }

    @Test
    void nonCanonicalEquivalentsExcludeTheCanonical() {
        for (UnificationGroup g : CompatRules.groups()) {
            assertTrue(
                    !g.nonCanonicalEquivalents().contains(g.canonical()),
                    "nonCanonicalEquivalents() must not contain the canonical for '" + g.group() + "'");
        }
    }
}
