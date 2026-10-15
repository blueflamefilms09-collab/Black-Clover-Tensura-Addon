package com.newuniverse.nusmp.boss.arc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The chest gem: window, critical hit, classification, body factor, the shatter and its paid-out burn. */
class CoreGemMathTest {
    @Test
    void theGemIsOutEightSecondsOfEveryFourteenAndClosedFirst() {
        assertFalse(CoreGemMath.exposed(0, false), "the clones come first");
        assertFalse(CoreGemMath.exposed(119, false));
        assertTrue(CoreGemMath.exposed(120, false));
        assertTrue(CoreGemMath.exposed(279, false));
        assertFalse(CoreGemMath.exposed(280, false), "closed again");
        assertFalse(CoreGemMath.exposed(399, false));
        assertTrue(CoreGemMath.exposed(400, false), "the next window");
        int open = 0;
        for (long t = 0; t < CoreGemMath.CYCLE_TICKS; t++) if (CoreGemMath.exposed(t, false)) open++;
        assertEquals(CoreGemMath.OPEN_TICKS, open);
    }

    @Test
    void channelingKeepsItOutAndTheWarningCounts() {
        assertTrue(CoreGemMath.exposed(10, true));
        assertFalse(CoreGemMath.exposed(-5, false));
        assertEquals(120, CoreGemMath.opensIn(0));
        assertEquals(20, CoreGemMath.opensIn(100));
        assertEquals(0, CoreGemMath.opensIn(200));
    }

    @Test
    void meleeCritFollowsVanilla() {
        assertTrue(CoreGemMath.meleeCrit(1f, 1.5f, false, false, false, false, false, false));
        assertFalse(CoreGemMath.meleeCrit(0.5f, 1.5f, false, false, false, false, false, false), "half swing");
        assertFalse(CoreGemMath.meleeCrit(1f, 0f, false, false, false, false, false, false), "not falling");
        assertFalse(CoreGemMath.meleeCrit(1f, 1.5f, true, false, false, false, false, false), "on the ground");
        assertFalse(CoreGemMath.meleeCrit(1f, 1.5f, false, true, false, false, false, false), "climbing");
        assertFalse(CoreGemMath.meleeCrit(1f, 1.5f, false, false, true, false, false, false), "in water");
        assertFalse(CoreGemMath.meleeCrit(1f, 1.5f, false, false, false, true, false, false), "blind");
        assertFalse(CoreGemMath.meleeCrit(1f, 1.5f, false, false, false, false, true, false), "riding");
        assertFalse(CoreGemMath.meleeCrit(1f, 1.5f, false, false, false, false, false, true), "sprinting");
    }

    @Test
    void classification() {
        assertEquals(CoreGemMath.Hit.HIDDEN, CoreGemMath.classify(false, true, true));
        assertEquals(CoreGemMath.Hit.NORMAL, CoreGemMath.classify(true, false, false));
        assertEquals(CoreGemMath.Hit.SHATTER, CoreGemMath.classify(true, true, false));
        assertEquals(CoreGemMath.Hit.SHATTER, CoreGemMath.classify(true, false, true));
    }

    @Test
    void bodyHitsAreReducedOnlyInTheLastAct() {
        assertEquals(1f, CoreGemMath.bodyFactor(LuciusArc.MESSIAH));
        assertEquals(1f, CoreGemMath.bodyFactor(LuciusArc.SOUL_CORRUPTOR));
        assertEquals(0.15f, CoreGemMath.bodyFactor(LuciusArc.CLONES));
    }

    @Test
    void shatterIsTwentyTwoPercentButNeverKills() {
        assertEquals(1056f, CoreGemMath.shatterDamage(4800f, 1400f), 1e-3f);
        assertEquals(499f, CoreGemMath.shatterDamage(4800f, 500f), 1e-3f, "leaves one health");
        assertEquals(0f, CoreGemMath.shatterDamage(4800f, 1f));
        assertEquals(0f, CoreGemMath.shatterDamage(4800f, 0f));
    }

    @Test
    void theBurnPaysOnlyWhatTheBudgetLetsThrough() {
        CoreGemMath.Burn b = new CoreGemMath.Burn();
        b.add(1000f);
        assertTrue(b.active());
        b.paid(300f);
        assertEquals(700f, b.pending(), 1e-3f);
        b.paid(0f);
        assertEquals(700f, b.want(), 1e-3f);
        b.paid(9999f);
        assertFalse(b.active());
        b.add(-5f);
        assertFalse(b.active());
    }
}
