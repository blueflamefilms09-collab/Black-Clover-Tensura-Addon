package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the nature-magic rebuild (Tree, Vine, Plant, Poison Plant, Mud, Mucus) must keep every old page id at its old place (saves, unlock bits, mastery and cooldowns depend on it) and only append,
 * stay inside the 24-page limit, and its armour and summon packing must stay what the server and the painters agree on. The decisions are pure ({@link TreeRules}), so they are pinned here.
 */
class TreeRulesTest {
    private static final List<String> MAGICS = List.of("TREE", "VINE", "PLANT", "POISON_PLANT", "MUD", "MUCUS");

    @Test
    void everyRebuiltBookKeepsItsOldIdsFirstAndAppendsTheNewOnes() {
        for (String m : MAGICS) {
            List<String> ids = TreeRules.expectedIds(m);
            assertNotNull(ids, m);
            assertNull(TreeRules.check(m, ids), m);
            List<String> old = m.equals("PLANT") ? TreeRules.PLANT_IDS : TreeRules.GENERIC_IDS;
            assertEquals(old, ids.subList(0, old.size()), m + " must keep the old ids first, in order");
            assertEquals(TreeRules.APPENDED.get(m), ids.subList(old.size(), ids.size()), m + " appends its new pages after them");
        }
    }

    @Test
    void theWikiFamilyIdsAreTheGenericFiveInTheirOldOrder() {
        assertEquals(List.of("attribute_bolt", "attribute_cut", "attribute_field", "attribute_domain", "attribute_guard"), TreeRules.GENERIC_IDS);
        assertEquals(List.of("guidepost", "hundred_flowers", "yggdrasil", "briar_prison"), TreeRules.PLANT_IDS);
    }

    @Test
    void aWrongOrderADuplicateOrTooManyPagesAreRefused() {
        List<String> ids = new ArrayList<>(TreeRules.expectedIds("TREE"));
        List<String> swapped = new ArrayList<>(ids);
        swapped.set(0, ids.get(1));
        swapped.set(1, ids.get(0));
        assertTrue(TreeRules.check("TREE", swapped).contains("must be"));
        List<String> dup = new ArrayList<>(ids);
        dup.set(5, dup.get(0));
        assertTrue(TreeRules.check("TREE", dup).contains("twice"));
        List<String> many = new ArrayList<>(ids);
        while (many.size() <= TreeRules.MAX_FAMILY) many.add("extra_" + many.size());
        assertTrue(TreeRules.check("TREE", many).contains("more than"));
        assertTrue(TreeRules.check("NOPE", ids).contains("unknown"));
        List<String> removed = new ArrayList<>(ids);
        removed.remove(0);
        assertTrue(TreeRules.check("TREE", removed) != null);
    }

    @Test
    void everyBookStaysUnderTheLimitAndNoIdIsUsedTwice() {
        for (String m : MAGICS) {
            List<String> ids = TreeRules.expectedIds(m);
            assertTrue(ids.size() <= TreeRules.MAX_FAMILY, m);
            assertEquals(ids.size(), new java.util.HashSet<>(ids).size(), m);
            for (String id : ids) assertTrue(id.matches("[a-z0-9_]+"), id + " is a lower snake_case save key");
        }
    }

    @Test
    void anArmourSoftensABlowAndTheHeavyOnesMoreThanTheLight() {
        for (String m : MAGICS) {
            float f = TreeRules.softening(m);
            assertTrue(f > 0.5f && f < 1f, m + " softens, never to nothing");
        }
        assertTrue(TreeRules.softening("MUCUS") < TreeRules.softening("MUD") && TreeRules.softening("MUD") < TreeRules.softening("TREE"));
        assertEquals(1f, TreeRules.softening("SOMETHING_ELSE"), 0f);
    }

    @Test
    void theSummonParamPacksVariantPhaseAndClockWithoutLosingAnything() {
        for (int variant = 0; variant < 4; variant++) for (int phase = 0; phase < 4; phase++) for (int since : new int[]{0, 1, 14, 63}) {
            int p = TreeRules.pack(variant, phase, since);
            assertEquals(variant, TreeRules.variantOf(p));
            assertEquals(phase, TreeRules.phaseOf(p));
            assertEquals(since, TreeRules.sinceOf(p));
        }
        assertEquals(63, TreeRules.sinceOf(TreeRules.pack(0, 1, 500)), "the clock saturates at 63");
        assertEquals(0, TreeRules.sinceOf(TreeRules.pack(0, 1, -5)), "and never goes negative");
        assertEquals(14592, TreeRules.pack(0, 1, 14), "the number the render-preview scenes use for a guardian 14 ticks into a slam");
    }
}
