package com.newuniverse.nusmp.arc.zagred;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the ward is the first four acts' whole defence (layers, grains, the unwritten window, the rewrite one layer short, the refresh, the anchor), so every rule of it is a test. */
class ZagredWardTest {
    static final float MAX = 1000f, SHARE = 0.045f, CAP = 45f;

    static ZagredWard fresh(int layers) {
        ZagredWard w = new ZagredWard();
        w.set(layers, MAX, SHARE, 0);
        return w;
    }

    @Test
    void aFreshWardHasFullLayersOfAShareOfHisHealth() {
        ZagredWard w = fresh(3);
        assertTrue(w.present());
        assertEquals(CAP, w.cap(), 1e-4f);
        assertEquals(3, w.layers(0));
        assertEquals(3, w.alive(0));
        for (int i = 0; i < 3; i++) assertEquals(1f, w.fraction(i), 1e-6f);
        assertEquals(0f, w.fraction(3), 1e-6f, "no fourth layer");
        assertFalse(w.reforming(0));
    }

    @Test
    void grainsAlternateAndTheInnermostIsArcane() {
        assertEquals(ZagredWard.Grain.ARCANE, ZagredWard.grainOfLayer(0));
        assertEquals(ZagredWard.Grain.KINETIC, ZagredWard.grainOfLayer(1));
        assertEquals(ZagredWard.Grain.ARCANE, ZagredWard.grainOfLayer(2));
    }

    @Test
    void aLayerTakesFullDamageFromItsOwnGrainAndFortyPercentFromTheOther() {
        ZagredWard own = fresh(3);                                            // the outermost layer (index 2) is arcane
        assertEquals(0f, own.absorb(CAP, ZagredWard.Grain.ARCANE, false, 1), 1e-5f);
        assertEquals(1, own.lastBroken());
        assertFalse(own.standing(2));
        assertTrue(own.standing(1));

        ZagredWard other = fresh(3);
        assertEquals(0f, other.absorb(CAP, ZagredWard.Grain.KINETIC, false, 1), 1e-5f);
        assertEquals(0, other.lastBroken(), "40% of the blow does not break it");
        assertEquals(1f - 0.4f, other.fraction(2), 1e-4f);
    }

    @Test
    void aBlowThatBreaksALayerCarriesOnIntoTheNextOne() {
        ZagredWard w = fresh(3);
        w.absorb(CAP * 1.5f, ZagredWard.Grain.ARCANE, false, 1);               // breaks layer 2 (45) and half of layer 1 at 40% (it is kinetic)
        assertFalse(w.standing(2));
        float expect = 1f - (CAP * 0.5f * 0.4f) / CAP;
        assertEquals(expect, w.fraction(1), 1e-4f);
        assertEquals(1, w.lastBroken());
    }

    @Test
    void antiMagicReachesThroughBothGrainsAtOneAndAHalf() {
        ZagredWard w = fresh(3);
        assertEquals(0f, w.absorb(30f, ZagredWard.Grain.KINETIC, true, 1), 1e-5f);
        assertFalse(w.standing(2), "30 x 1.5 = 45: exactly one layer, whatever its grain");
        ZagredWard v = fresh(3);
        v.absorb(30f, ZagredWard.Grain.ARCANE, true, 1);
        assertFalse(v.standing(2));
    }

    @Test
    void breakingTheLastLayerOpensTheUnwrittenWindowAndPassesTheSurplus() {
        ZagredWard w = fresh(1);
        float through = w.absorb(CAP + 10f, ZagredWard.Grain.ARCANE, false, 100);
        assertEquals(10f * ZagredWard.UNWRITTEN, through, 1e-4f, "the surplus passes at 150%");
        assertTrue(w.lastReformStarted());
        assertTrue(w.reforming(100));
        assertEquals(0, w.layers(100), "nothing to draw while he is unwritten");
        assertTrue(w.reforming(100 + ZagredWard.REFORM_TICKS - 1));
        assertFalse(w.reforming(100 + ZagredWard.REFORM_TICKS));
    }

    @Test
    void aLastLayerThatBreaksExactlyStillOpensTheWindow() {
        ZagredWard w = fresh(1);
        assertEquals(0f, w.absorb(CAP, ZagredWard.Grain.ARCANE, false, 5), 1e-5f);
        assertTrue(w.lastReformStarted(), "no layer left: he must rewrite at once, not at the next blow");
    }

    @Test
    void anUnwrittenBodyTakesOneAndAHalfTimes() {
        ZagredWard w = fresh(1);
        w.absorb(CAP + 1f, ZagredWard.Grain.ARCANE, false, 0);
        assertEquals(30f, w.absorb(20f, ZagredWard.Grain.KINETIC, false, 10), 1e-4f);
        assertFalse(w.lastReformStarted());
        assertEquals(0, w.lastBroken());
    }

    @Test
    void theRewriteComesOneLayerShortAndItsWindowEndsWithAnEvent() {
        ZagredWard w = fresh(3);
        for (int i = 0; i < 6; i++) w.absorb(CAP * 5, ZagredWard.Grain.ARCANE, true, 10);   // empties everything
        assertTrue(w.reforming(10));
        for (long t = 11; t < 10 + ZagredWard.REFORM_TICKS; t++) assertFalse(w.tick(t, false, false), "no event before the window ends");
        assertTrue(w.tick(10 + ZagredWard.REFORM_TICKS, false, false), "the ward is written again");
        long now = 10 + ZagredWard.REFORM_TICKS;
        assertEquals(2, w.effective(now), "one layer short");
        assertEquals(2, w.alive(now));
        assertEquals(3, w.effective(now + ZagredWard.PENALTY_TICKS), "whole again after twenty seconds");
        assertFalse(w.tick(now + 1, false, false));
    }

    @Test
    void theWeakestLayerGetsAThirdBackOnSchedule() {
        ZagredWard w = fresh(3);
        w.absorb(CAP * 0.5f, ZagredWard.Grain.ARCANE, false, 1);                // outer layer at half
        float before = w.fraction(2);
        assertFalse(w.tick(ZagredWard.REFRESH_TICKS - 1, false, false));
        assertEquals(before, w.fraction(2), 1e-6f, "not yet");
        w.tick(ZagredWard.REFRESH_TICKS, false, false);
        assertEquals(before + ZagredWard.REFRESH_SHARE, w.fraction(2), 1e-4f);
    }

    @Test
    void anAnchorMakesTheRefreshComeSooner() {
        ZagredWard slow = fresh(3), fast = fresh(3);
        slow.absorb(CAP * 0.9f, ZagredWard.Grain.ARCANE, false, 1);
        fast.absorb(CAP * 0.9f, ZagredWard.Grain.ARCANE, false, 1);
        slow.tick(ZagredWard.REFRESH_TICKS, false, false);
        fast.tick(ZagredWard.REFRESH_TICKS, false, true);                       // first refresh of both, then the next interval differs
        slow.absorb(CAP * 0.9f, ZagredWard.Grain.ARCANE, false, ZagredWard.REFRESH_TICKS + 1);
        fast.absorb(CAP * 0.9f, ZagredWard.Grain.ARCANE, false, ZagredWard.REFRESH_TICKS + 1);
        float s0 = slow.fraction(1) + slow.fraction(2), f0 = fast.fraction(1) + fast.fraction(2);
        long t = ZagredWard.REFRESH_TICKS + ZagredWard.REFRESH_ANCHORED_TICKS;
        slow.tick(t, false, false);
        fast.tick(t, false, true);
        assertEquals(s0, slow.fraction(1) + slow.fraction(2), 1e-6f, "unanchored: nothing yet");
        assertTrue(fast.fraction(1) + fast.fraction(2) > f0, "anchored: the second refresh has come");
    }

    @Test
    void channelingAWordStopsTheRefresh() {
        ZagredWard w = fresh(3);
        w.absorb(CAP * 0.5f, ZagredWard.Grain.ARCANE, false, 1);
        float before = w.fraction(2);
        w.tick(ZagredWard.REFRESH_TICKS + 5, true, false);
        assertEquals(before, w.fraction(2), 1e-6f);
    }

    @Test
    void breakOneTakesTheOutermostStandingLayerAndEmptyingThemOpensTheWindow() {
        ZagredWard w = fresh(2);
        assertTrue(w.breakOne(1));
        assertFalse(w.standing(1));
        assertTrue(w.standing(0));
        assertFalse(w.lastReformStarted());
        assertTrue(w.breakOne(1));
        assertTrue(w.lastReformStarted(), "the last layer gone: he is unwritten");
        assertFalse(w.breakOne(2), "nothing to break while he rewrites");
    }

    @Test
    void noWardPassesTheWholeBlow() {
        ZagredWard w = fresh(0);
        assertFalse(w.present());
        assertEquals(17f, w.absorb(17f, ZagredWard.Grain.KINETIC, false, 3), 1e-6f);
        assertFalse(w.tick(1000, false, false));
        assertFalse(w.breakOne(1));
        assertEquals(0, w.layers(5));
    }

    @Test
    void loadRestoresASavedWardAndClampsWhatIsWrong() {
        ZagredWard w = new ZagredWard();
        w.load(3, 50f, new float[]{50f, 20f, 999f, 7f}, 0);
        assertEquals(3, w.count());
        assertEquals(1f, w.fraction(0), 1e-6f);
        assertEquals(0.4f, w.fraction(1), 1e-6f);
        assertEquals(1f, w.fraction(2), 1e-6f, "clamped to the cap");
        assertEquals(0f, w.fraction(3), 1e-6f, "beyond the count");
        w.load(9, 0f, null, 0);
        assertEquals(ZagredWard.MAX, w.count());
        assertEquals(1f, w.cap(), 1e-6f);
    }
}
