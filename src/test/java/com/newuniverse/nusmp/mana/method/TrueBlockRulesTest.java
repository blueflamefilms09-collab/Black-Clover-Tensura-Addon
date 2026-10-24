package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.TrueBlockRules.Kind;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The real blocks of True Magic: which element places which block, how many, and the caps. */
class TrueBlockRulesTest {
    @Test
    void waterFireEarthAndPlantPlaceBlocksWindAndLightningDoNot() {
        assertEquals(Kind.FIRE, TrueBlockRules.kindOf(RuneElement.FIRE));
        assertEquals(Kind.WATER, TrueBlockRules.kindOf(RuneElement.WATER));
        assertEquals(Kind.STONE, TrueBlockRules.kindOf(RuneElement.EARTH));
        assertEquals(Kind.MOSS, TrueBlockRules.kindOf(RuneElement.PLANT));
        assertEquals(Kind.NONE, TrueBlockRules.kindOf(RuneElement.WIND));
        assertEquals(Kind.NONE, TrueBlockRules.kindOf(RuneElement.LIGHTNING));
        assertEquals(Kind.NONE, TrueBlockRules.kindOf(null));
        assertEquals("minecraft:water", TrueBlockRules.blockId(Kind.WATER), "True Water places Water Source blocks");
        assertEquals("", TrueBlockRules.blockId(Kind.NONE));
        for (Kind k : Kind.values()) {
            if (k == Kind.NONE) { assertEquals(0, TrueBlockRules.blocksWanted(k)); continue; }
            assertTrue(TrueBlockRules.blocksWanted(k) > 0 && TrueBlockRules.blocksWanted(k) <= TrueBlockRules.PER_CAST, k + " blocks");
            assertTrue(TrueBlockRules.blockId(k).startsWith("minecraft:"), "a vanilla block: " + k);
        }
        assertFalse(TrueBlockRules.needsGround(Kind.WATER));
        assertTrue(TrueBlockRules.needsGround(Kind.FIRE) && TrueBlockRules.needsGround(Kind.STONE) && TrueBlockRules.needsGround(Kind.MOSS));
    }

    @Test
    void theCellsAreDistinctAndCloseToTheImpact() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < TrueBlockRules.cellCount(); i++) {
            int[] c = TrueBlockRules.cell(i);
            assertTrue(Math.abs(c[0]) <= 1 && Math.abs(c[1]) <= 1, "within one block of the impact");
            assertTrue(seen.add(c[0] + "," + c[1]), "cell " + i + " repeats");
        }
        assertEquals(0, TrueBlockRules.cell(0)[0]);
        assertEquals(0, TrueBlockRules.cell(0)[1]);
        assertTrue(TrueBlockRules.cellCount() >= TrueBlockRules.PER_CAST, "enough cells for the biggest cast");
        assertEquals(TrueBlockRules.cell(0)[0], TrueBlockRules.cell(-TrueBlockRules.cellCount())[0], "an odd index wraps, never throws");
    }

    @Test
    void theCapsHoldAtEveryLevel() {
        assertEquals(5, TrueBlockRules.allowed(5, 0, 0, TrueBlockRules.PER_CASTER, TrueBlockRules.GLOBAL));
        assertEquals(TrueBlockRules.PER_CAST, TrueBlockRules.allowed(100, 0, 0, TrueBlockRules.PER_CASTER, TrueBlockRules.GLOBAL), "one cast never more than the per-cast cap");
        assertEquals(3, TrueBlockRules.allowed(7, TrueBlockRules.PER_CASTER - 3, 0, TrueBlockRules.PER_CASTER, TrueBlockRules.GLOBAL), "one caster never more than the per-caster cap");
        assertEquals(2, TrueBlockRules.allowed(7, 0, TrueBlockRules.GLOBAL - 2, TrueBlockRules.PER_CASTER, TrueBlockRules.GLOBAL), "the world never more than the global cap");
        assertEquals(0, TrueBlockRules.allowed(7, TrueBlockRules.PER_CASTER, 0, TrueBlockRules.PER_CASTER, TrueBlockRules.GLOBAL));
        assertEquals(0, TrueBlockRules.allowed(7, 0, TrueBlockRules.GLOBAL + 50, TrueBlockRules.PER_CASTER, TrueBlockRules.GLOBAL), "over the cap is none, not negative");
        assertEquals(0, TrueBlockRules.allowed(-3, 0, 0, TrueBlockRules.PER_CASTER, TrueBlockRules.GLOBAL));
        assertTrue(TrueBlockRules.PER_CASTER >= TrueBlockRules.PER_CAST && TrueBlockRules.GLOBAL >= TrueBlockRules.PER_CASTER);
    }

    @Test
    void theLifetimeIsClamped() {
        assertEquals(TrueBlockRules.MIN_SECONDS, TrueBlockRules.seconds(-9));
        assertEquals(TrueBlockRules.MAX_SECONDS, TrueBlockRules.seconds(10_000_000));
        assertEquals(TrueBlockRules.DEFAULT_SECONDS, TrueBlockRules.seconds(TrueBlockRules.DEFAULT_SECONDS));
        assertTrue(TrueBlockRules.MAX_SECONDS <= 300, "nothing True Magic builds stays longer than five minutes");
    }
}
