package com.newuniverse.nusmp.relics.djinn;

import org.junit.jupiter.api.Test;

import static com.newuniverse.nusmp.relics.djinn.BondRules.*;
import static org.junit.jupiter.api.Assertions.*;

/** The exclusive bond: one of a Spirit, a Devil or a Djinn, unless the multi-bond relic is worn. */
class BondRulesTest {
    private static final int[] KINDS = {SPIRIT, DEVIL, DJINN};

    @Test
    void truthTableOfHeldTimesMultiBondTimesKind() {
        for (int held = 0; held < 8; held++) {
            for (int want : KINDS) {
                int others = held & ~want;
                assertEquals(others == 0, canBind(held, want, false), "no relic: held=" + held + " want=" + want);
                assertTrue(canBind(held, want, true), "the multi-bond relic opens every door: held=" + held + " want=" + want);
            }
        }
    }

    @Test
    void anEmptyPlayerCanTakeAnyOneBond() {
        for (int want : KINDS) assertTrue(canBind(0, want, false));
    }

    @Test
    void holdingOneKindBlocksTheOtherTwoButNotItself() {
        assertTrue(canBind(DJINN, DJINN, false));
        assertFalse(canBind(DJINN, SPIRIT, false));
        assertFalse(canBind(DJINN, DEVIL, false));
        assertFalse(canBind(SPIRIT, DJINN, false));
        assertFalse(canBind(DEVIL, DJINN, false));
        assertFalse(canBind(SPIRIT, DEVIL, false));
    }

    @Test
    void thePriceOfHoldingMoreThanOneKind() {
        assertEquals(0, extraBonds(DJINN, true));
        assertEquals(1, extraBonds(DJINN | SPIRIT, true));
        assertEquals(2, extraBonds(DJINN | SPIRIT | DEVIL, true));
        assertEquals(0, extraBonds(DJINN | SPIRIT, false), "without the relic nothing is paid (nothing should be held)");
        assertEquals(1.0, drainMultiplier(DJINN, true));
        assertEquals(1.4, drainMultiplier(DJINN | DEVIL, true));
        assertEquals(1.0, trustGainMultiplier(SPIRIT, true));
        assertEquals(0.5, trustGainMultiplier(SPIRIT | DEVIL, true));
        assertEquals(0.25, trustGainMultiplier(SPIRIT | DEVIL | DJINN, true));
    }

    @Test
    void noNaturalRegenerationInMasoOnlyForTheMultiBonded() {
        assertTrue(regenBlocked(DJINN | SPIRIT, true, DjinnPhaseRules.FULL));
        assertFalse(regenBlocked(DJINN | SPIRIT, true, DjinnPhaseRules.HALF));
        assertFalse(regenBlocked(DJINN, true, DjinnPhaseRules.FULL), "one bond is not a price");
        assertFalse(regenBlocked(DJINN | SPIRIT, false, DjinnPhaseRules.FULL));
    }

    @Test
    void countOnlyLooksAtTheThreeBits() {
        assertEquals(0, count(0));
        assertEquals(3, count(7));
        assertEquals(1, count(8 | DJINN), "a stray high bit is ignored");
    }
}
