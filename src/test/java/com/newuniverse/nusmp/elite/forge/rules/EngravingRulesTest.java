package com.newuniverse.nusmp.elite.forge.rules;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the engraving rules cap how many Tensura engravings a forged item may carry by its quality and decide at what level they are applied, and which come from the
 * specialization's pool; all of it is pure and tested here (the Minecraft half only turns a plan into enchantments).
 */
class EngravingRulesTest {
    @Test
    void slotsGrowWithQuality() {
        int prev = -1;
        for (Quality q : Quality.values()) {
            int s = EngravingRules.slots(q);
            assertTrue(s > prev);
            prev = s;
        }
        assertEquals(0, EngravingRules.slots(Quality.COMMON));
        assertEquals(5, EngravingRules.slots(Quality.MYTHICAL));
    }

    @Test
    void aMatchingSpecializationAddsOneSlotButNeverPastTheCap() {
        assertEquals(3, EngravingRules.totalSlots(Quality.EPIC, null, Category.WEAPON));
        assertEquals(4, EngravingRules.totalSlots(Quality.EPIC, Specialization.WEAPONSMITH, Category.WEAPON));
        assertEquals(3, EngravingRules.totalSlots(Quality.EPIC, Specialization.WEAPONSMITH, Category.ARMOR));
        assertEquals(6, EngravingRules.totalSlots(Quality.MYTHICAL, Specialization.ARMORSMITH, Category.ARMOR));
        assertEquals(5, EngravingRules.totalSlots(Quality.MYTHICAL, Specialization.RUNESMITH, Category.RUNE), "the Runesmith gets levels, not slots");
        assertEquals(0, EngravingRules.totalSlots(Quality.COMMON, Specialization.WEAPONSMITH, Category.WEAPON), "a Common item has no slot even for the right smith");
        for (Quality q : Quality.values()) for (Specialization s : Specialization.values()) for (Category c : Category.values())
            assertTrue(EngravingRules.totalSlots(q, s, c) <= EngravingRules.MAX_SLOTS);
    }

    @Test
    void theForgeFillsHalfTheSlotsRoundedUp() {
        assertEquals(0, EngravingRules.initialCount(0));
        assertEquals(1, EngravingRules.initialCount(1));
        assertEquals(1, EngravingRules.initialCount(2));
        assertEquals(2, EngravingRules.initialCount(3));
        assertEquals(3, EngravingRules.initialCount(6));
    }

    @Test
    void levelRisesWithQualityAndTheRunesmith() {
        assertEquals(1, EngravingRules.levelFor(Quality.RARE, null));
        assertEquals(2, EngravingRules.levelFor(Quality.EPIC, null));
        assertEquals(2, EngravingRules.levelFor(Quality.LEGENDARY, Specialization.WEAPONSMITH));
        assertEquals(3, EngravingRules.levelFor(Quality.MYTHICAL, null));
        assertEquals(4, EngravingRules.levelFor(Quality.MYTHICAL, Specialization.RUNESMITH));
        assertEquals(2, EngravingRules.levelFor(Quality.COMMON, Specialization.RUNESMITH));
    }

    @Test
    void thePlanIsReproducibleAndStaysInThePoolAndTheFreeSlots() {
        List<EngravingRules.Pick> a = EngravingRules.plan(Quality.LEGENDARY, Specialization.RUNESMITH, Category.RUNE, 0, 99, 1234L, List.of());
        List<EngravingRules.Pick> b = EngravingRules.plan(Quality.LEGENDARY, Specialization.RUNESMITH, Category.RUNE, 0, 99, 1234L, List.of());
        assertEquals(a, b);
        assertEquals(4, a.size(), "the free slots bound the plan");
        Set<String> seen = new HashSet<>();
        for (EngravingRules.Pick p : a) {
            assertTrue(Specialization.RUNESMITH.pool().contains(p.id()));
            assertTrue(seen.add(p.id()), "no engraving twice");
            assertEquals(EngravingRules.levelFor(Quality.LEGENDARY, Specialization.RUNESMITH), p.level());
        }
        assertEquals(1, EngravingRules.plan(Quality.LEGENDARY, Specialization.RUNESMITH, Category.RUNE, 3, 99, 1234L, List.of()).size(), "already applied ones use up slots");
        assertEquals(0, EngravingRules.plan(Quality.LEGENDARY, Specialization.RUNESMITH, Category.RUNE, 4, 99, 1234L, List.of()).size());
        assertEquals(2, EngravingRules.plan(Quality.LEGENDARY, Specialization.RUNESMITH, Category.RUNE, 0, 2, 1234L, List.of()).size(), "want bounds it too");
        assertEquals(0, EngravingRules.plan(Quality.COMMON, null, Category.WEAPON, 0, 5, 1L, List.of()).size());
        // excluded ids (already on the item) are never picked
        List<EngravingRules.Pick> c = EngravingRules.plan(Quality.MYTHICAL, null, Category.TOOL, 0, 9, 7L, List.of("sturdy"));
        for (EngravingRules.Pick p : c) assertTrue(!p.id().equals("sturdy"));
        assertTrue(c.size() <= Specialization.BASIC_POOL.size() - 1);
    }

    @Test
    void differentSeedsGiveDifferentOrders() {
        Set<String> firsts = new HashSet<>();
        for (long seed = 0; seed < 60; seed++) firsts.add(EngravingRules.order(Specialization.WEAPONSMITH, seed, null).get(0));
        assertTrue(firsts.size() >= 3, "the shuffle must vary with the seed");
        assertEquals(Specialization.WEAPONSMITH.pool().size(), EngravingRules.order(Specialization.WEAPONSMITH, 3L, null).size());
    }
}
