package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.LightningCharge.Strike;
import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 3.6.0: True Lightning grows with the charge (more bolts, taller, wider, longer, bigger splash, more arcs on the rune), always inside the hard caps. */
class LightningChargeTest {
    private static final int MIN = RuneCharge.DEFAULT_MIN_HOLD, MAX = RuneCharge.DEFAULT_MAX_HOLD;
    private static final RuneSpec LIGHTNING = new RuneSpec(RuneElement.LIGHTNING, RuneMods.ALL, true);
    private static final RuneSpec FIRE = new RuneSpec(RuneElement.FIRE, RuneMods.ALL, true);

    @Test
    void theStrikeGrowsWithEveryStageAndNeverPassesTheCaps() {
        Strike tap = LightningCharge.strike(RuneCharge.of(0, MIN, MAX));
        Strike mid = LightningCharge.strike(RuneCharge.of((MIN + MAX) / 2, MIN, MAX));
        Strike full = LightningCharge.strike(RuneCharge.of(MAX, MIN, MAX));
        assertEquals(1, tap.bolts());
        assertEquals(2, mid.bolts());
        assertEquals(3, full.bolts());
        assertTrue(tap.height() < mid.height() && mid.height() < full.height());
        assertTrue(tap.width() < mid.width() && mid.width() < full.width());
        assertTrue(tap.ticks() <= mid.ticks() && mid.ticks() <= full.ticks());
        for (int h = 0; h <= RuneCharge.HOLD_CAP; h++) {
            Strike s = LightningCharge.strike(RuneCharge.of(h, MIN, MAX));
            assertTrue(s.bolts() >= 1 && s.bolts() <= LightningCharge.MAX_BOLTS);
            assertTrue(s.height() <= LightningCharge.MAX_HEIGHT && s.width() <= LightningCharge.MAX_WIDTH);
            assertTrue(s.ticks() >= 1 && s.ticks() <= 40);
        }
    }

    @Test
    void theOldSpellGetsOneBoltOfTheBaseSize() {
        Strike s = LightningCharge.strike(Charge.LEGACY);
        assertEquals(1, s.bolts());
        assertTrue(s.height() >= LightningCharge.BASE_HEIGHT && s.height() <= LightningCharge.MAX_HEIGHT);
        assertEquals(1, LightningCharge.strike(null).bolts());
    }

    @Test
    void onlyTheLightningElementWithAMeasuredHoldIsScaled() {
        Charge full = RuneCharge.of(MAX, MIN, MAX);
        assertTrue(LightningCharge.scales(LIGHTNING, full));
        assertFalse(LightningCharge.scales(FIRE, full));
        assertFalse(LightningCharge.scales(LIGHTNING, Charge.LEGACY));
        assertFalse(LightningCharge.scales(null, full));
        assertEquals(1.0, LightningCharge.radiusMult(FIRE, full), 0.0);
        assertEquals(1.0, LightningCharge.radiusMult(LIGHTNING, Charge.LEGACY), 0.0);
    }

    @Test
    void theRadiusGrowsFromOneToAtMostTheCap() {
        double last = 0.99;
        for (int h = 0; h <= RuneCharge.HOLD_CAP; h++) {
            double m = LightningCharge.radiusMult(LIGHTNING, RuneCharge.of(h, MIN, MAX));
            assertTrue(m >= last - 1e-12 && m >= 1.0 && m <= LightningCharge.MAX_RADIUS_MULT, "x" + m + " at " + h);
            last = m;
        }
        assertEquals(1.0, LightningCharge.radiusMult(LIGHTNING, RuneCharge.of(0, MIN, MAX)), 1e-9);
        assertEquals(1.35, LightningCharge.radiusMult(LIGHTNING, RuneCharge.of(MAX, MIN, MAX)), 1e-9);
    }

    @Test
    void theRadiusReachesTheBoltInsideItsOwnHardCaps() {
        Charge full = RuneCharge.of(MAX, MIN, MAX);
        RuneCommands.BoltState plain = RuneCommands.inject(LIGHTNING, full, false);
        RuneCommands.BoltState grown = RuneCommands.inject(LIGHTNING, full, true);
        assertTrue(grown.hitRadius(30.0) >= plain.hitRadius(30.0), "the hit volume does not shrink");
        assertTrue(grown.hitRadius(30.0) <= RuneCommands.MAX_HIT_RADIUS + 1e-9);
        assertTrue(grown.splashRadius(30.0) <= RuneCommands.MAX_SPLASH + 1e-9);
        assertEquals(plain.splashRadius(10.0), RuneCommands.inject(LIGHTNING, full, false).splashRadius(10.0), 1e-12, "the flag off is the 3.5.0 bolt");
    }

    @Test
    void theBoltsStandApartAndTheFirstIsAtTheCentre() {
        Strike s = LightningCharge.strike(RuneCharge.of(MAX, MIN, MAX));
        double[] a = new double[2], b = new double[2], c = new double[2];
        LightningCharge.boltOffset(s, 0, a);
        LightningCharge.boltOffset(s, 1, b);
        LightningCharge.boltOffset(s, 2, c);
        assertEquals(0.0, a[0], 0.0);
        assertEquals(0.0, a[1], 0.0);
        assertEquals(s.spread(), Math.hypot(b[0], b[1]), 1e-9);
        assertEquals(s.spread(), Math.hypot(c[0], c[1]), 1e-9);
        assertTrue(Math.hypot(b[0] - c[0], b[1] - c[1]) > 0.5 * s.spread(), "the side bolts do not overlap");
    }

    @Test
    void theArcsOfTheRuneFollowTheRingsThatAreWritten() {
        assertEquals(0, LightningCharge.arcsReached(0.0, 0.0, 0.0));
        assertEquals(1, LightningCharge.arcsReached(1.0, 0.0, 0.0));
        assertEquals(2, LightningCharge.arcsReached(1.0, 1.0, 0.2));
        assertEquals(3, LightningCharge.arcsReached(1.0, 1.0, 0.9));
        for (int r = 1; r <= 3; r++) assertTrue(LightningCharge.arcsReached(1, 1, 1) <= RuneGlyphLayout.MAX_ARCS);
        assertTrue(LightningCharge.arcLight(1) < LightningCharge.arcLight(2) && LightningCharge.arcLight(2) < LightningCharge.arcLight(3));
        assertTrue(LightningCharge.arcWidth(1) < LightningCharge.arcWidth(3));
        assertTrue(LightningCharge.arcJag(1) < LightningCharge.arcJag(3));
        assertTrue(LightningCharge.startRing(1) < LightningCharge.startRing(2) && LightningCharge.startRing(2) < LightningCharge.startRing(3), "the arcs start further out");
        assertEquals(2, LightningCharge.startRing(7));
        assertEquals(0, LightningCharge.startRing(0));
    }

    /** The layout's progress is a point of the timeline (hand ring 0.03..0.48), NOT a 0..1 share: the arcs must be asked with the share (found by the preview, where no arc was drawn). */
    @Test
    void theLayoutAsksTheArcsWithTheWrittenShareOfEachRingNotWithTheTimelineProgress() {
        for (int g = 0; g < 3; g++) {
            assertEquals(0.0, RuneGlyphLayout.writtenOf(g, RuneGlyphLayout.progressOf(g, 0.0)), 1e-9);
            assertEquals(0.5, RuneGlyphLayout.writtenOf(g, RuneGlyphLayout.progressOf(g, 0.5)), 1e-9);
            assertEquals(1.0, RuneGlyphLayout.writtenOf(g, RuneGlyphLayout.progressOf(g, 1.0)), 1e-9);
            assertEquals(0.0, RuneGlyphLayout.writtenOf(g, -5.0), 0.0);
            assertEquals(1.0, RuneGlyphLayout.writtenOf(g, 5.0), 0.0);
        }
        assertEquals(0.0, RuneGlyphLayout.writtenOf(0, Double.NaN), 0.0);
        // a tap (hand ring finished), a half hold (hand and middle ring) and a full hold, as the hold view reports them
        int tap = LightningCharge.arcsReached(RuneGlyphLayout.writtenOf(0, RuneGlyphLayout.progressOf(0, 1.0)), RuneGlyphLayout.writtenOf(1, RuneGlyphLayout.progressOf(1, 0.0)), RuneGlyphLayout.writtenOf(2, RuneGlyphLayout.progressOf(2, 0.0)));
        int half = LightningCharge.arcsReached(RuneGlyphLayout.writtenOf(0, RuneGlyphLayout.progressOf(0, 1.0)), RuneGlyphLayout.writtenOf(1, RuneGlyphLayout.progressOf(1, 1.0)), RuneGlyphLayout.writtenOf(2, RuneGlyphLayout.progressOf(2, 0.3)));
        int full = LightningCharge.arcsReached(RuneGlyphLayout.writtenOf(0, RuneGlyphLayout.progressOf(0, 1.0)), RuneGlyphLayout.writtenOf(1, RuneGlyphLayout.progressOf(1, 1.0)), RuneGlyphLayout.writtenOf(2, RuneGlyphLayout.progressOf(2, 1.0)));
        assertEquals(1, tap);
        assertEquals(2, half);
        assertEquals(3, full);
        assertEquals(0, LightningCharge.arcsReached(RuneGlyphLayout.progressOf(0, 1.0), 0.0, 0.0), "the raw timeline progress of a finished hand ring (0.48) never reaches the threshold: the old call drew no arc");
    }
}
