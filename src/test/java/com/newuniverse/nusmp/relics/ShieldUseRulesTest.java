package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.relics.ShieldUseRules.Use;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 1.110.0: what a click on a shield relic does (the plain click always raises it; the old relic click is the sneaking one). */
class ShieldUseRulesTest {
    @Test
    void aPlainClickAlwaysRaisesTheShield() {
        for (boolean acts : new boolean[]{false, true}) {
            assertEquals(Use.BLOCK, ShieldUseRules.decide(false, acts, false, false), "no attunement needed");
            assertEquals(Use.BLOCK, ShieldUseRules.decide(false, acts, true, true), "attuned");
            assertEquals(Use.BLOCK_HINT, ShieldUseRules.decide(false, acts, true, false), "unattuned: it still blocks, with the hint");
        }
    }

    @Test
    void aSneakingClickKeepsTheOldRelicClick() {
        assertEquals(Use.RELIC_CLICK, ShieldUseRules.decide(true, true, false, false), "a shield with an act: sneak + use is the act");
        assertEquals(Use.RELIC_CLICK, ShieldUseRules.decide(true, false, true, false), "unattuned: sneak + hold is the attunement ritual");
        assertEquals(Use.RELIC_CLICK, ShieldUseRules.decide(true, true, true, false));
        assertEquals(Use.BLOCK, ShieldUseRules.decide(true, false, false, false), "a shield without an act blocks while sneaking, like vanilla's");
        assertEquals(Use.BLOCK, ShieldUseRules.decide(true, false, true, true));
    }

    @Test
    void theRitualOnlyRunsWhileSneaking() {
        assertTrue(ShieldUseRules.ritualRuns(true));
        assertFalse(ShieldUseRules.ritualRuns(false));
    }
}
