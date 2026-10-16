package com.newuniverse.nusmp.boss.god;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** What counts as a massive channel, the cooldown book, the mirror numbers. */
class CounterRulesTest {
    @Test
    void aHeavyPageHeldLongEnoughIsMassive() {
        assertTrue(CounterRules.massiveGrimoire(30, false, 20, 20, true));
        assertTrue(CounterRules.massiveGrimoire(5, true, 20, 20, true), "a zone or signature page is heavy whatever its cost");
    }

    @Test
    void aLightPageOrAShortHoldOrAPageThatCannotBeCastIsNot() {
        assertFalse(CounterRules.massiveGrimoire(10, false, 40, 20, true), "light");
        assertFalse(CounterRules.massiveGrimoire(30, false, 5, 20, true), "just pressed");
        assertFalse(CounterRules.massiveGrimoire(30, false, 40, 20, false), "on cooldown or sealed: nothing is being channelled");
    }

    @Test
    void theHoldNeededIsThreeQuartersOfTheCastTimeAndAtLeastTen() {
        assertFalse(CounterRules.massiveGrimoire(30, false, 14, 20, true));
        assertTrue(CounterRules.massiveGrimoire(30, false, 15, 20, true));
        assertFalse(CounterRules.massiveGrimoire(30, false, 9, 4, true));
        assertTrue(CounterRules.massiveGrimoire(30, false, 10, 4, true));
    }

    @Test
    void aSkillMustBeHeldThirtyTicksAndBeAnActiveKind() {
        assertFalse(CounterRules.massiveSkill("ultimate", 29));
        assertTrue(CounterRules.massiveSkill("ultimate", 30));
        assertTrue(CounterRules.massiveSkill("unique", 60));
        assertFalse(CounterRules.massiveSkill("resistance", 400));
        assertFalse(CounterRules.massiveSkill("intrinsic", 400));
        assertFalse(CounterRules.massiveSkill(null, 400));
    }

    @Test
    void onlyAnUltimateIsReversed() {
        assertTrue(CounterRules.reversible("ultimate"));
        assertFalse(CounterRules.reversible("unique"));
        assertFalse(CounterRules.reversible("extra"));
        assertFalse(CounterRules.reversible(null));
    }

    @Test
    void theMirrorIsAShareOfTheCastersMaximumAndNeverMoreThanFortyPercent() {
        assertEquals(12f, CounterRules.mirrorDamage(200f, 6.0), 1e-4f);
        assertEquals(80f, CounterRules.mirrorDamage(200f, 90.0), 1e-3f);
        assertEquals(0f, CounterRules.mirrorDamage(200f, 0.0));
        assertEquals(0f, CounterRules.mirrorDamage(0f, 6.0));
    }

    @Test
    void aReflectedBlowReturnsHalfCappedAtFortyPercent() {
        assertEquals(50f, CounterRules.swapMirror(100f, 1000f), 1e-4f);
        assertEquals(40f, CounterRules.swapMirror(10000f, 100f), 1e-4f);
        assertEquals(0f, CounterRules.swapMirror(0f, 100f));
        assertEquals(0f, CounterRules.swapMirror(10f, 0f));
    }

    @Test
    void theCooldownsAreEightTenAndTwelveSeconds() {
        assertEquals(160, CounterRules.CHECKMATE_COOLDOWN);
        assertEquals(200, CounterRules.REVERSAL_COOLDOWN);
        assertEquals(240, CounterRules.SWAP_COOLDOWN);
    }

    @Test
    void theCooldownBookHoldsPerKey() {
        CounterRules.Cooldowns<String> c = new CounterRules.Cooldowns<>();
        assertTrue(c.ready("a", 0));
        assertTrue(c.use("a", 100, 160));
        assertFalse(c.use("a", 259, 160));
        assertTrue(c.ready("b", 100), "another target is not affected");
        assertTrue(c.use("a", 260, 160));
        assertEquals(420L, c.readyAt("a"));
        c.clear();
        assertEquals(0, c.size());
    }

    @Test
    void theCooldownBookPrunesWhenItGrows() {
        CounterRules.Cooldowns<Integer> c = new CounterRules.Cooldowns<>();
        for (int i = 0; i < 300; i++) c.use(i, i, 5);
        assertTrue(c.size() <= 135, "old entries are dropped: " + c.size());
    }

    @Test
    void theChannelWatchNeedsTheHoldAndForgetsOnRelease() {
        CounterRules.ChannelWatch w = new CounterRules.ChannelWatch();
        assertFalse(w.massive(100));
        w.begin(100, "ultimate");
        assertTrue(w.held());
        assertFalse(w.massive(120));
        assertTrue(w.massive(130));
        w.end();
        assertFalse(w.held());
        assertFalse(w.massive(500));
        w.begin(0, "resistance");
        assertFalse(w.massive(500));
    }
}
