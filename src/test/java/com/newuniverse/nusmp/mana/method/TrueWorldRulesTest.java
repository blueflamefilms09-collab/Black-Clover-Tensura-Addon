package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.TrueWorldRules.Material;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The plant's cage and the lightning allowance: the shape, the caps, who gets caged, how many bolts a second. */
class TrueWorldRulesTest {
    @Test
    void theCageIsTwoRingsAndARoofWithoutTheTargetsOwnCells() {
        assertEquals(17, TrueWorldRules.CAGE_CELLS);
        Set<String> seen = new HashSet<>();
        boolean roof = false;
        int logs = 0, leaves = 0;
        for (int i = 0; i < TrueWorldRules.CAGE_CELLS; i++) {
            int[] c = TrueWorldRules.cageCell(i);
            assertTrue(Math.abs(c[0]) <= 1 && Math.abs(c[2]) <= 1 && c[1] >= 0 && c[1] <= 2, "within the 3x3x3 round the target");
            assertTrue(seen.add(c[0] + "," + c[1] + "," + c[2]), "cell " + i + " repeats");
            assertFalse(c[0] == 0 && c[2] == 0 && c[1] < 2, "the cells the target stands in are free");
            if (c[1] == 2) { roof = true; assertEquals(Material.LOG, TrueWorldRules.cageMaterial(i), "a roof of wood"); }
            if (TrueWorldRules.cageMaterial(i) == Material.LOG) logs++; else leaves++;
        }
        assertTrue(roof);
        assertTrue(logs > 0 && leaves > 0, "posts of logs, sides of leaves: leaves can be broken by hand, so nobody is stuck");
        assertEquals(TrueWorldRules.cageCell(0)[0], TrueWorldRules.cageCell(-TrueWorldRules.CAGE_CELLS)[0], "an odd index wraps, never throws");
    }

    @Test
    void theWallsComeBeforeTheRoofSoATruncatedCageIsStillAWall() {
        for (int i = 0; i < 16; i++) assertTrue(TrueWorldRules.cageCell(i)[1] <= 1, "cell " + i);
        assertEquals(2, TrueWorldRules.cageCell(16)[1]);
    }

    @Test
    void theCapsHold() {
        assertEquals(Math.min(TrueWorldRules.CAGE_CELLS, TrueWorldRules.CAGE_PER_CAST), TrueWorldRules.allowedCage(0, 0), "a free caster gets the whole cage, never more than the per-cast cap");
        assertEquals(TrueWorldRules.CAGE_PER_CASTER - 30, TrueWorldRules.allowedCage(30, 0), "a caster with 30 standing may add the rest of its cap");
        assertEquals(0, TrueWorldRules.allowedCage(TrueWorldRules.CAGE_PER_CASTER, 0));
        assertEquals(0, TrueWorldRules.allowedCage(0, TrueWorldRules.CAGE_GLOBAL));
        assertEquals(4, TrueWorldRules.allowedCage(0, TrueWorldRules.CAGE_GLOBAL - 4));
        assertEquals(0, TrueWorldRules.allowedCage(500, 500), "over the cap is zero, never negative");
        for (int a = 0; a < 300; a += 17)
            for (int b = 0; b < 300; b += 23) {
                int n = TrueWorldRules.allowedCage(a, b);
                assertTrue(n >= 0 && n <= TrueWorldRules.CAGE_PER_CAST);
            }
    }

    @Test
    void theCageLastsAtMostThirtySecondsAndFollowsTheBlockTime() {
        assertTrue(TrueWorldRules.cageSeconds(5) <= 5);
        assertTrue(TrueWorldRules.cageSeconds(10_000) <= TrueWorldRules.CAGE_MAX_SECONDS);
        assertTrue(TrueWorldRules.cageSeconds(-3) >= 1);
    }

    @Test
    void onlyAStruckFoeIsCagedAndOnlyWithTheSettingOn() {
        assertTrue(TrueWorldRules.cageWanted(true, true, true));
        assertFalse(TrueWorldRules.cageWanted(false, true, true), "the setting is off: the moss carpet of 3.4.0");
        assertFalse(TrueWorldRules.cageWanted(true, false, false), "a wall hit has nothing to trap");
        assertFalse(TrueWorldRules.cageWanted(true, true, false), "never an ally or a bystander");
    }

    @Test
    void lightningIsRateLimitedPerSecondOnTheWholeServer() {
        long[] w = {Long.MIN_VALUE, 0};
        for (int i = 0; i < TrueWorldRules.LIGHTNING_PER_SECOND; i++) assertTrue(TrueWorldRules.takeBolt(100, w), "bolt " + i);
        assertFalse(TrueWorldRules.takeBolt(100, w));
        assertFalse(TrueWorldRules.takeBolt(119, w), "same second");
        assertTrue(TrueWorldRules.takeBolt(120, w), "the next second");
        long[] w2 = {Long.MIN_VALUE, 0};
        int taken = 0;
        for (long t = 0; t < 200; t++)
            for (int k = 0; k < 10; k++) if (TrueWorldRules.takeBolt(t, w2)) taken++;
        assertEquals(10 * TrueWorldRules.LIGHTNING_PER_SECOND, taken, "ten seconds, three a second, however many ask");
    }

    @Test
    void theFullVanillaBoltNeedsEverythingToBeSafe() {
        assertTrue(TrueWorldRules.realBoltAllowed(true, true, true, 0));
        assertFalse(TrueWorldRules.realBoltAllowed(false, true, true, 0), "the setting is off (the default)");
        assertFalse(TrueWorldRules.realBoltAllowed(true, false, true, 0), "doFireTick off");
        assertFalse(TrueWorldRules.realBoltAllowed(true, true, false, 0), "trueMagic.placeFire off");
        assertFalse(TrueWorldRules.realBoltAllowed(true, true, true, 1), "somebody who is no foe is close");
        assertTrue(TrueWorldRules.bystander(false) && !TrueWorldRules.bystander(true));
    }
}
