package com.newuniverse.nusmp.charset;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The acquisition rules (docs/magic/character_spells.md section 3), without a game: a character's spell opens by his own rule, never earlier. */
class SpellRuleTest {
    /** A player in a map. */
    static final class F implements RuleFacts {
        double mastery;
        int trust;
        final Map<String, Integer> uses = new HashMap<>();
        final Set<String> open = new HashSet<>(), flags = new HashSet<>();
        @Override public double mastery() { return mastery; }
        @Override public int uses(String p) { return uses.getOrDefault(p, 0); }
        @Override public boolean pageOpen(String p) { return open.contains(p); }
        @Override public int trust() { return trust; }
        @Override public boolean flag(String n) { return flags.contains(n); }
    }

    @Test
    void startIsAlwaysOpen() {
        assertTrue(new SpellRule.Start().test(new F()));
    }

    @Test
    void masteryNeedsTheBarToReachTheLevel() {
        F f = new F();
        SpellRule r = new SpellRule.Mastery(0.5);
        f.mastery = 0.49;
        assertFalse(r.test(f));
        f.mastery = 0.5;
        assertTrue(r.test(f));
    }

    @Test
    void pageUsesCountsCasts() {
        F f = new F();
        SpellRule r = new SpellRule.PageUses("spirit_dive", 2);
        f.uses.put("spirit_dive", 1);
        assertFalse(r.test(f));
        f.uses.put("spirit_dive", 2);
        assertTrue(r.test(f));
    }

    @Test
    void pageOpenAndTrustAndFlagReadTheirFacts() {
        F f = new F();
        assertFalse(new SpellRule.PageOpen("a").test(f));
        f.open.add("a");
        assertTrue(new SpellRule.PageOpen("a").test(f));
        f.trust = 40;
        assertTrue(new SpellRule.Trust(40).test(f));
        assertFalse(new SpellRule.Trust(41).test(f));
        assertFalse(new SpellRule.Flag("saint_stage").test(f));
        f.flags.add("saint_stage");
        assertTrue(new SpellRule.Flag("saint_stage").test(f));
    }

    @Test
    void spiritOfZephyrWaitsForTheSpiritDive() {
        // the owner's example, in the shape the Yuno file uses
        SpellRule zephyr = new SpellRule.All(List.of(new SpellRule.PageUses("spirit_dive", 2), new SpellRule.Mastery(0.4)));
        F f = new F();
        f.mastery = 1.0;
        f.trust = 100;
        f.flags.addAll(List.of("spirit_bond", "saint_stage"));
        assertFalse(zephyr.test(f), "no Spirit Dive yet: a maxed grimoire and a full bond are not enough");
        f.uses.put("spirit_dive", 1);
        assertFalse(zephyr.test(f), "one Dive is not enough");
        f.uses.put("spirit_dive", 2);
        assertTrue(zephyr.test(f));
        f.mastery = 0.3;
        assertFalse(zephyr.test(f), "and the mastery still counts");
    }

    @Test
    void allNeedsEveryRuleAndAnyNeedsOne() {
        F f = new F();
        f.mastery = 0.6;
        SpellRule all = new SpellRule.All(List.of(new SpellRule.Mastery(0.5), new SpellRule.Flag("mana_zone")));
        SpellRule any = new SpellRule.Any(List.of(new SpellRule.Mastery(0.5), new SpellRule.Flag("mana_zone")));
        assertFalse(all.test(f));
        assertTrue(any.test(f));
        f.flags.add("mana_zone");
        assertTrue(all.test(f));
    }

    @Test
    void pagesAreCollectedFromNestedRules() {
        SpellRule r = new SpellRule.All(List.of(new SpellRule.PageUses("a", 1), new SpellRule.Any(List.of(new SpellRule.PageOpen("b"), new SpellRule.Mastery(0.1)))));
        assertEquals(Set.of("a", "b"), r.pages());
    }

    @Test
    void descriptionsAreReadable() {
        assertEquals("spirit_dive cast 2 times", new SpellRule.PageUses("spirit_dive", 2).describe());
        assertEquals("spirit_dive cast 1 time", new SpellRule.PageUses("spirit_dive", 1).describe());
        assertEquals("mastery 40% and saint stage", new SpellRule.All(List.of(new SpellRule.Mastery(0.4), new SpellRule.Flag("saint_stage"))).describe());
    }
}
