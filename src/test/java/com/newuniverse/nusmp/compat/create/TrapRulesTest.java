package com.newuniverse.nusmp.compat.create;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: which trap, where, how often and how hard are decisions a server owner tunes and a party lives or dies by, so every one is pinned. */
class TrapRulesTest {
    private static final Set<TrapFamily> ALL = EnumSet.allOf(TrapFamily.class);

    @Test
    void onlyTheRightRoomsAreEligible() {
        for (String ok : new String[] {"corridor", "link", "junction", "treasure", "trap", "hall"}) assertTrue(TrapRules.eligible(ok), ok);
        for (String no : new String[] {"landing", "safe", "stairwell", "arena", "antechamber", "vault", "onward", "set", "trial", "puzzle", "cavern", "cap", "", null})
            assertFalse(TrapRules.eligible(no), String.valueOf(no));
    }

    @Test
    void chanceGrowsWithDepthAndScalesWithDensity() {
        assertTrue(TrapRules.chance("corridor", 100, 1.0) > TrapRules.chance("corridor", 1, 1.0));
        assertTrue(TrapRules.chance("corridor", 10, 2.0) > TrapRules.chance("corridor", 10, 1.0));
        assertEquals(0.0, TrapRules.chance("corridor", 10, 0.0));
        assertEquals(0.0, TrapRules.chance("safe", 10, 4.0));
        assertTrue(TrapRules.chance("trap", 1000, 4.0) <= 0.9);
        assertEquals(TrapRules.chance("corridor", 200, 1.0), TrapRules.chance("corridor", 5000, 1.0), "depth stops counting at 200");
    }

    @Test
    void theSectorCapFollowsRoomsAndDensity() {
        assertEquals(0, TrapRules.maxPerSector(100, 0.0));
        assertEquals(0, TrapRules.maxPerSector(0, 1.0));
        assertEquals(1, TrapRules.maxPerSector(8, 0.5));
        assertEquals(0, TrapRules.maxPerSector(7, 0.4), "too few rooms for a trap at a low density");
        assertTrue(TrapRules.maxPerSector(100, 1.0) >= TrapRules.maxPerSector(50, 1.0));
        assertTrue(TrapRules.maxPerSector(100000, 4.0) <= 24);
    }

    @Test
    void familiesAreGatedByDepthAndByTheServersSwitches() {
        for (double r = 0; r < 1; r += 0.01) {
            TrapFamily f = TrapRules.pickFamily("gravity", 1, 1, "corridor", r, ALL);
            assertNotNull(f);
            assertTrue(f.minDepth() <= 1, "floor 1 drew " + f);
        }
        assertNull(TrapRules.pickFamily("gravity", 1, 1, "corridor", 0.5, EnumSet.of(TrapFamily.PRESS)), "press needs floor 4");
        assertNull(TrapRules.pickFamily("gravity", 50, 1, "corridor", 0.5, EnumSet.noneOf(TrapFamily.class)));
        assertNotNull(TrapRules.pickFamily("gravity", 50, 1, "corridor", 0.5, null), "null = every family");
        for (double r = 0; r < 1; r += 0.05) assertEquals(TrapFamily.POUR, TrapRules.pickFamily("gravity", 50, 1, "corridor", r, EnumSet.of(TrapFamily.POUR)));
    }

    @Test
    void aThemeLeansTowardsItsOwnMachinery() {
        int forgePress = 0, plainPress = 0, stormSeq = 0, plainSeq = 0, frostPour = 0, flamePour = 0;
        for (int i = 0; i < 1000; i++) {
            double r = (i + 0.5) / 1000.0;
            if (TrapRules.pickFamily("dwarven_forgehold", 50, 1, "corridor", r, ALL) == TrapFamily.PRESS) forgePress++;
            if (TrapRules.pickFamily("gravity", 50, 1, "corridor", r, ALL) == TrapFamily.PRESS) plainPress++;
            if (TrapRules.pickFamily("storm", 50, 1, "corridor", r, ALL) == TrapFamily.SEQUENCE) stormSeq++;
            if (TrapRules.pickFamily("gravity", 50, 1, "corridor", r, ALL) == TrapFamily.SEQUENCE) plainSeq++;
            if (TrapRules.pickFamily("frost", 50, 1, "corridor", r, ALL) == TrapFamily.POUR) frostPour++;
            if (TrapRules.pickFamily("infernal_foundry", 50, 1, "corridor", r, ALL) == TrapFamily.POUR) flamePour++;
        }
        assertTrue(forgePress > plainPress, forgePress + " vs " + plainPress);
        assertTrue(stormSeq > plainSeq, stormSeq + " vs " + plainSeq);
        assertTrue(flamePour > frostPour, flamePour + " vs " + frostPour);
    }

    @Test
    void theDrawIsDeterministicAndCoversEveryAllowedFamily() {
        for (double r = 0; r < 1; r += 0.07) assertEquals(TrapRules.pickFamily("storm", 30, 5, "hall", r, ALL), TrapRules.pickFamily("storm", 30, 5, "hall", r, ALL));
        Set<TrapFamily> seen = EnumSet.noneOf(TrapFamily.class);
        for (int i = 0; i < 2000; i++) seen.add(TrapRules.pickFamily("gravity", 100, 5, "corridor", (i + 0.5) / 2000.0, ALL));
        assertEquals(ALL, seen);
    }

    @Test
    void triggersAndLengthsStayInRange() {
        for (double r = 0; r < 1; r += 0.01) {
            assertNotNull(TrapRules.pickTrigger(r));
            int c = TrapRules.pickCore(r);
            assertTrue(c >= TrapRules.MIN_CORE && c <= TrapRules.MAX_CORE);
            assertEquals(c + 2, TrapRules.stripLength(c));
        }
        assertEquals(TriggerKind.PLATE, TrapRules.pickTrigger(0.0));
        assertEquals(TriggerKind.PROXIMITY, TrapRules.pickTrigger(0.99));
        assertEquals(TrapRules.MIN_CORE, TrapRules.pickCore(0.0));
        assertEquals(TrapRules.MAX_CORE, TrapRules.pickCore(0.999));
    }

    @Test
    void damageGrowsWithDepthAndThreat() {
        for (HazardKind k : HazardKind.values()) {
            assertTrue(TrapRules.damage(k, 100, 1, 1.0) > TrapRules.damage(k, 1, 1, 1.0), k.id());
            assertTrue(TrapRules.damage(k, 10, 20, 1.0) > TrapRules.damage(k, 10, 1, 1.0), k.id());
            assertEquals(0.0, TrapRules.damage(k, 10, 1, 0.0), k.id());
        }
        assertTrue(TrapRules.damage(HazardKind.CRUSH, 1, 1, 1.0) > TrapRules.damage(HazardKind.SCALD, 1, 1, 1.0));
    }

    @Test
    void aTrapNeverKillsAndNeverTakesMoreThanItsShares() {
        float max = 20f;
        for (float hp = 0.5f; hp <= max; hp += 0.5f) {
            for (double wanted : new double[] {1, 5, 20, 200}) {
                float d = TrapRules.safeDamage(wanted, hp, max, 0f);
                assertTrue(d >= 0f);
                assertTrue(d <= max * TrapRules.MAX_HIT_SHARE + 1e-4, "one hit took " + d);
                assertTrue(hp - d > 0f, "hp " + hp + " wanted " + wanted + " left " + (hp - d));
            }
        }
        assertEquals(0f, TrapRules.safeDamage(10, 4f, 20f, 0f), "mercy below 20%");
        assertEquals(0f, TrapRules.safeDamage(10, 20f, 20f, 14f), "the run's share is spent");
        assertEquals(0f, TrapRules.safeDamage(0, 20f, 20f, 0f));
        float total = 0f, hp = 20f;
        for (int i = 0; i < 50; i++) {
            float d = TrapRules.safeDamage(30, hp, 20f, total);
            total += d;
            hp -= d;
        }
        assertTrue(total <= 20f * TrapRules.MAX_RUN_SHARE + 1e-3, "a run took " + total);
        assertTrue(hp > 0f);
    }
}
