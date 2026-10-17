package com.newuniverse.nusmp.arc.danmachi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the red head and the black hide are counters the player has to find, so every threshold of them (what counts, the window, the plates, the time the hide stays
 * broken, the reset for a new fight) is pinned here.
 */
class HeadHideTest {
    // ------------------------------------------------------------------ the red head
    @Test
    void blowsOutsideTheWindowCountForNothing() {
        HeadCut h = new HeadCut();
        assertFalse(h.windowOpen(0));
        assertFalse(h.onHit(0, HeadCut.Kind.PHYSICAL, 5000f, 2000f), "the head is not reared");
        h.open(100, 60);
        assertTrue(h.windowOpen(100));
        assertTrue(h.windowOpen(159));
        assertFalse(h.windowOpen(160));
        assertFalse(h.onHit(160, HeadCut.Kind.PHYSICAL, 5000f, 2000f), "the window closed");
        assertFalse(h.cut());
    }

    @Test
    void enoughPhysicalDamageInTheWindowCutsTheHead() {
        HeadCut h = new HeadCut();
        h.open(0, 100);
        float max = 2000f, need = (float) (HeadCut.CUT_FRACTION * max);
        assertFalse(h.onHit(10, HeadCut.Kind.PHYSICAL, need - 1f, max));
        assertFalse(h.cut());
        assertTrue(h.onHit(20, HeadCut.Kind.PHYSICAL, 1f, max), "the last point cuts it");
        assertTrue(h.cut());
        assertFalse(h.windowOpen(21), "a cut head has no window");
        assertFalse(h.onHit(22, HeadCut.Kind.PHYSICAL, 999f, max), "and counts nothing more");
    }

    @Test
    void antiMagicCountsDoubleAndSpellsNothing() {
        HeadCut a = new HeadCut();
        a.open(0, 100);
        float max = 2000f, half = (float) (HeadCut.CUT_FRACTION * max / 2.0);
        assertTrue(a.onHit(5, HeadCut.Kind.ANTI, half, max), "half the damage, twice the weight");
        HeadCut m = new HeadCut();
        m.open(0, 100);
        for (int i = 0; i < 50; i++) assertFalse(m.onHit(i, HeadCut.Kind.MAGIC, 1000f, max), "the mist eats every spell");
        assertEquals(0.0, m.worn(), 1e-12);
        assertEquals(1.0, HeadCut.weight(HeadCut.Kind.PHYSICAL), 1e-12);
        assertEquals(2.0, HeadCut.weight(HeadCut.Kind.ANTI), 1e-12);
        assertEquals(0.0, HeadCut.weight(HeadCut.Kind.MAGIC), 1e-12);
        assertEquals(1.0, HeadCut.weight(null), 1e-12, "an unknown kind is a plain blow");
    }

    @Test
    void aNewWindowStartsFromNothing() {
        HeadCut h = new HeadCut();
        float max = 2000f, need = (float) (HeadCut.CUT_FRACTION * max);
        h.open(0, 50);
        h.onHit(10, HeadCut.Kind.PHYSICAL, need - 5f, max);
        h.open(200, 50);
        assertEquals(0.0, h.worn(), 1e-12, "the wear of the first window is forgotten");
        assertFalse(h.onHit(210, HeadCut.Kind.PHYSICAL, 10f, max));
    }

    @Test
    void theFightTakesTheHeadAtTheEighthPercentStepAndNotBefore() {
        for (int power : new int[]{0, 20, 40, 60, 79}) assertFalse(HeadCut.mustCut(power), "power " + power);
        for (int power : new int[]{80, 100}) assertTrue(HeadCut.mustCut(power), "power " + power);
        assertTrue(HeadCut.FORCE_CUT_POWER > 60, "the Twin Calamity (60) still has the red head the first time it is thrown");
    }

    @Test
    void theFightCanCutTheHeadItself() {
        HeadCut h = new HeadCut();
        assertTrue(h.forceCut());
        assertTrue(h.cut());
        assertFalse(h.forceCut(), "once");
        h.open(0, 100);
        assertFalse(h.windowOpen(1));
        h.reset();
        assertFalse(h.cut());
        h.open(0, 100);
        assertTrue(h.windowOpen(1), "a new fight has a head again");
    }

    @Test
    void badNumbersAreHarmless() {
        HeadCut h = new HeadCut();
        h.open(0, 0);
        assertTrue(h.windowOpen(0), "a window is at least a tick");
        assertFalse(h.onHit(0, HeadCut.Kind.PHYSICAL, 0f, 2000f));
        assertFalse(h.onHit(0, HeadCut.Kind.PHYSICAL, -5f, 2000f));
        assertFalse(h.onHit(0, HeadCut.Kind.PHYSICAL, 50f, 0f));
        assertFalse(h.cut());
    }

    // ------------------------------------------------------------------ the black hide
    @Test
    void aPlainBlowDoesHalfAndAnythingElseDoesFull() {
        HideState h = new HideState();
        float max = 1900f;
        HideState.Result plain = h.onHit(0, true, 5f, max);
        assertEquals(HideState.PLAIN_FACTOR, plain.factor(), 1e-9);
        assertFalse(plain.splitPlate());
        HideState.Result spell = h.onHit(1, false, 500f, max);
        assertEquals(1.0f, spell.factor(), 1e-9);
        assertFalse(spell.splitPlate(), "a spell never splits a plate: it does not need to");
        assertEquals(0, h.platesSplit(2));
    }

    @Test
    void aHeavyPlainBlowSplitsAPlateAndLandsInFull() {
        HideState h = new HideState();
        float max = 1900f, heavy = HideState.PLATE_FRACTION * max;
        HideState.Result r = h.onHit(10, true, heavy, max);
        assertTrue(r.splitPlate());
        assertFalse(r.brokeHide());
        assertEquals(1.0f, r.factor(), 1e-9);
        assertEquals(1, h.platesSplit(11));
        assertEquals(HideState.PLAIN_FACTOR, h.onHit(12, true, heavy - 1f, max).factor(), 1e-9, "just under heavy");
    }

    @Test
    void threePlatesInSixSecondsBreakTheHide() {
        HideState h = new HideState();
        float max = 1900f, heavy = HideState.PLATE_FRACTION * max;
        assertFalse(h.onHit(0, true, heavy, max).brokeHide());
        assertFalse(h.onHit(50, true, heavy, max).brokeHide());
        HideState.Result third = h.onHit(100, true, heavy, max);
        assertTrue(third.brokeHide());
        assertTrue(h.broken(101));
        assertEquals(HideState.BROKEN_TICKS, h.brokenLeft(100));
        assertEquals(1.0f, h.onHit(120, true, 1f, max).factor(), 1e-9, "a feather does full damage while the hide is broken");
        assertFalse(h.broken(100 + HideState.BROKEN_TICKS));
        assertEquals(HideState.PLAIN_FACTOR, h.onHit(100 + HideState.BROKEN_TICKS, true, 1f, max).factor(), 1e-9, "and the hide is back");
        assertEquals(0, h.platesSplit(100 + HideState.BROKEN_TICKS), "the plates grew back too");
    }

    @Test
    void slowHeavyBlowsNeverBreakTheHide() {
        HideState h = new HideState();
        float max = 1900f, heavy = HideState.PLATE_FRACTION * max;
        for (int i = 0; i < 12; i++) assertFalse(h.onHit(i * 70L, true, heavy, max).brokeHide(), "a heavy blow every 3.5 s: only two plates are ever open");
        assertFalse(h.broken(900));
    }

    @Test
    void resetPutsTheHideBack() {
        HideState h = new HideState();
        float max = 1900f, heavy = HideState.PLATE_FRACTION * max;
        h.onHit(0, true, heavy, max);
        h.onHit(5, true, heavy, max);
        h.onHit(10, true, heavy, max);
        assertTrue(h.broken(11));
        h.reset();
        assertFalse(h.broken(11));
        assertEquals(0, h.platesSplit(11));
        assertEquals(HideState.PLAIN_FACTOR, h.onHit(12, true, 1f, max).factor(), 1e-9);
        assertEquals(HideState.PLAIN_FACTOR, h.onHit(13, true, 5f, 0f).factor(), 1e-9, "no known maximum health: nothing is heavy");
    }
}
