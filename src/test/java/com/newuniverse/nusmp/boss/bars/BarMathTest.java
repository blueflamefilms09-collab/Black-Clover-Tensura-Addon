package com.newuniverse.nusmp.boss.bars;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the pure maths behind the bar: the phase notches ({@link PhaseMarkers}), the damage-lag chip ({@link GhostBar}), the layout and motion numbers ({@link BarLayoutMath}) and the damage ticker
 * ({@link DpsMeter}). Each rule the owner asked for is one test: notches at the phase thresholds, a chip that holds and then drains slower than the health fell and vanishes on a heal, a fill that is
 * never empty while the boss lives, a compact stack from three bars, a deterministic shatter that returns to nothing.
 */
class BarMathTest {
    // ---------------------------------------------------------------- phase markers
    @Test
    void notchesSitAtTheThresholdsOfTheWindow() {
        assertEquals(0, PhaseMarkers.markerX(0));
        assertEquals(BarLayoutMath.WIN_W, PhaseMarkers.markerX(100));
        assertEquals(Math.round(66 * BarLayoutMath.WIN_W / 100f), PhaseMarkers.markerX(66));
        assertEquals(98, PhaseMarkers.markerX(50), "half of 196");
        assertEquals(0, PhaseMarkers.markerX(-5), "out of range is held at the ends");
    }

    @Test
    void thePhaseIsReadFromTheHealth() {
        List<Integer> th = List.of(66, 33);
        assertEquals(1, PhaseMarkers.phaseAt(1.0f, th));
        assertEquals(1, PhaseMarkers.phaseAt(0.67f, th));
        assertEquals(2, PhaseMarkers.phaseAt(0.66f, th), "the boss is in phase 2 AT its threshold");
        assertEquals(2, PhaseMarkers.phaseAt(0.40f, th));
        assertEquals(3, PhaseMarkers.phaseAt(0.33f, th));
        assertEquals(3, PhaseMarkers.phaseAt(0.01f, th));
        assertEquals(1, PhaseMarkers.phaseAt(0.2f, List.of()), "a one-phase boss has one phase");
        assertTrue(PhaseMarkers.passed(0.5f, 66));
        assertFalse(PhaseMarkers.passed(0.7f, 66));
    }

    @Test
    void romanNumeralsAndThresholdsFromAProfile() {
        assertEquals("I", PhaseMarkers.roman(1));
        assertEquals("IV", PhaseMarkers.roman(4));
        assertEquals("IX", PhaseMarkers.roman(9));
        assertEquals("", PhaseMarkers.roman(0));
        assertEquals("IX", PhaseMarkers.roman(40), "held at the largest numeral");
        assertEquals(List.of(66, 33), PhaseMarkers.fromShares(1.0, 0.66, 0.33), "the opening phase has no notch");
        assertEquals(List.of(), PhaseMarkers.fromShares(1.0));
    }

    // ---------------------------------------------------------------- ghost bar
    @Test
    void theChipHoldsThenDrainsSlowerThanTheHealthFell() {
        GhostBar g = new GhostBar();
        g.reset(1f);
        double t = 100;
        g.update(t, 1f);
        g.update(t += 1, 0.6f);                                       // a hit: 40 percent in one tick
        assertEquals(1.0f, g.ghost(), 1e-6f, "the chip stays where the health was");
        assertEquals(0.4f, g.chip(), 1e-4f);
        for (int i = 0; i < 10; i++) g.update(t += 1, 0.6f);          // inside the hold
        assertEquals(1.0f, g.ghost(), 1e-6f, "still held");
        for (int i = 0; i < 10; i++) g.update(t += 1, 0.6f);          // the hold is over
        assertTrue(g.ghost() < 1.0f && g.ghost() > 0.6f, "draining");
        float after20 = g.ghost();
        for (int i = 0; i < 400; i++) g.update(t += 1, 0.6f);
        assertEquals(0.6f, g.ghost(), 1e-6f, "the chip is gone in the end, never below the health");
        assertTrue(after20 > 0.75f, "slow: after twenty ticks (nine of them draining) more than half of the chip is still there: " + after20);
    }

    @Test
    void aHealLeavesNoChipAndTheGhostNeverPassesTheHealth() {
        GhostBar g = new GhostBar();
        g.reset(0.5f);
        g.update(10, 0.5f);
        g.update(11, 0.9f);
        assertEquals(0.9f, g.ghost(), 1e-6f);
        assertEquals(0f, g.chip(), 1e-6f);
        g.update(12, 0.2f);
        g.update(13, 0.3f);                                           // a little heal after a hit: the chip is still over the health
        assertTrue(g.ghost() >= g.health());
        assertEquals(0.3f, g.health(), 1e-6f);
    }

    @Test
    void theChipIsFrameRateIndependentEnough() {
        GhostBar slow = new GhostBar(), fast = new GhostBar();
        slow.reset(1f);
        fast.reset(1f);
        slow.update(0, 1f);
        fast.update(0, 1f);
        slow.update(1, 0.5f);
        fast.update(1, 0.5f);
        for (int i = 2; i <= 60; i++) slow.update(i, 0.5f);                       // one update per tick
        for (int i = 2; i <= 60 * 4; i++) fast.update(i / 4.0 + 0.75, 0.5f);       // four per tick
        assertEquals(slow.ghost(), fast.ghost(), 0.05f);
    }

    // ---------------------------------------------------------------- layout
    @Test
    void theFillIsNeverEmptyWhileTheBossLives() {
        assertEquals(0, BarLayoutMath.fillPixels(0f));
        assertEquals(1, BarLayoutMath.fillPixels(0.0004f));
        assertEquals(BarLayoutMath.WIN_W, BarLayoutMath.fillPixels(1f));
        assertEquals(BarLayoutMath.WIN_W, BarLayoutMath.fillPixels(7f));
        assertEquals(98, BarLayoutMath.fillPixels(0.5f));
        assertEquals(1, BarLayoutMath.percent(0.001f), "a living boss never reads 0%");
        assertEquals(0, BarLayoutMath.percent(0f));
        assertEquals(100, BarLayoutMath.percent(1f));
        assertEquals(73, BarLayoutMath.percent(0.731f));
    }

    @Test
    void threeBarsAreCompactAndTheStackStopsBeforeTheMiddleOfTheScreen() {
        assertFalse(BarLayoutMath.compact(1));
        assertFalse(BarLayoutMath.compact(2));
        assertTrue(BarLayoutMath.compact(3));
        assertEquals(BarLayoutMath.STEP, BarLayoutMath.step(false));
        assertEquals(BarLayoutMath.COMPACT_STEP, BarLayoutMath.step(true));
        assertTrue(BarLayoutMath.STEP > BarLayoutMath.FULL_H && BarLayoutMath.COMPACT_STEP > BarLayoutMath.COMPACT_H);
        assertTrue(BarLayoutMath.fits(4, 270));
        assertTrue(BarLayoutMath.fits(120, 270));
        assertFalse(BarLayoutMath.fits(200, 270));
    }

    @Test
    void theGeometryAgreesWithItself() {
        assertTrue(BarLayoutMath.WIN_X + BarLayoutMath.WIN_W < BarLayoutMath.WIDTH);
        assertTrue(BarLayoutMath.SOCK_X + BarLayoutMath.SOCK_R < BarLayoutMath.WIN_X, "the socket ends before the window starts");
        assertTrue(BarLayoutMath.PLATE_Y + BarLayoutMath.PLATE_H < BarLayoutMath.WIN_Y);
        assertTrue(BarLayoutMath.CAST_Y >= BarLayoutMath.FRAME_H && BarLayoutMath.ROW_Y > BarLayoutMath.CAST_Y + BarLayoutMath.CAST_H - 1);
        assertTrue(BarLayoutMath.LAYER_Y[1] >= BarLayoutMath.LAYER_Y[0] + BarLayoutMath.LAYER_H && BarLayoutMath.LAYER_Y[2] >= BarLayoutMath.LAYER_Y[1] + BarLayoutMath.LAYER_H, "the three layers do not overlap");
        assertTrue(BarLayoutMath.LAYER_Y[2] + BarLayoutMath.LAYER_H <= BarLayoutMath.ATLAS_H);
        assertEquals(BarLayoutMath.WIDTH * 2, BarLayoutMath.ATLAS_W);
        assertEquals(BarLayoutMath.FRAME_H * 2, BarLayoutMath.LAYER_H);
    }

    @Test
    void theEntranceEasesOutAndTheLowHealthPulseOnlyRunsBelowAQuarter() {
        assertEquals(0f, BarLayoutMath.entrance(0), 1e-6f);
        assertEquals(1f, BarLayoutMath.entrance(BarLayoutMath.ENTER_TICKS), 1e-6f);
        assertEquals(1f, BarLayoutMath.entrance(500), 1e-6f);
        assertTrue(BarLayoutMath.entrance(6) > 6f / BarLayoutMath.ENTER_TICKS, "ease out: faster than linear at the start");
        assertEquals(0f, BarLayoutMath.lowPulse(0.5f, 12.3), 0f);
        assertEquals(0f, BarLayoutMath.lowPulse(0f, 12.3), 0f);
        float max = 0f;
        for (int i = 0; i < 400; i++) max = Math.max(max, BarLayoutMath.lowPulse(0.1f, i * 0.25));
        assertTrue(max > 0.3f && max <= 1f, "it pulses: " + max);
        float g = BarLayoutMath.glowPulse(5, false), r = BarLayoutMath.glowPulse(5, true);
        assertTrue(g >= 0.55f && g <= 1f && r >= 0.55f && r <= 1f);
    }

    @Test
    void theShatterIsDeterministicAndComesBackTogether() {
        for (int c = 0; c < 12; c++) {
            assertEquals(BarLayoutMath.shatterDx(c, 12, 0.3f, 99L), BarLayoutMath.shatterDx(c, 12, 0.3f, 99L), 0f);
            assertEquals(0f, BarLayoutMath.shatterDx(c, 12, 0f, 99L), 1e-6f, "starts whole");
            assertEquals(0f, BarLayoutMath.shatterDx(c, 12, 1f, 99L), 1e-5f, "and ends whole");
        }
        assertTrue(Math.abs(BarLayoutMath.shatterDx(0, 12, 0.5f, 5L)) > 5f, "the outer strips fly out");
        assertTrue(BarLayoutMath.shatterDx(0, 12, 0.5f, 5L) < 0 && BarLayoutMath.shatterDx(11, 12, 0.5f, 5L) > 0, "left strips left, right strips right");
        assertTrue(BarLayoutMath.shatterDy(3, 1f, 5L) > BarLayoutMath.shatterDy(3, 0.2f, 5L), "they fall");
    }

    @Test
    void theRingCountsDown() {
        assertEquals(12, BarLayoutMath.ringDots(12, 100, 100));
        assertEquals(6, BarLayoutMath.ringDots(12, 50, 100));
        assertEquals(1, BarLayoutMath.ringDots(12, 1, 100));
        assertEquals(0, BarLayoutMath.ringDots(12, 0, 100));
        assertEquals(0, BarLayoutMath.ringDots(12, 10, 0));
        assertEquals(12, BarLayoutMath.ringDots(12, 900, 100), "never more than the ring has");
    }

    // ---------------------------------------------------------------- the ticker
    @Test
    void theDamageTickerReadsPercentPerSecond() {
        DpsMeter m = new DpsMeter();
        m.tick(0, 1f);
        float hp = 1f;
        for (int t = 1; t <= 60; t++) m.tick(t, hp -= 0.001f);              // 6 percent over three seconds = 2 percent per second
        assertEquals(2f, m.percentPerSecond(), 0.1f);
        for (int t = 61; t <= 130; t++) m.tick(t, hp);                       // nothing for a while: the window empties
        assertEquals(0f, m.percentPerSecond(), 0.01f);
        m.tick(131, hp + 0.3f);                                              // a heal is not negative damage
        assertEquals(0f, m.percentPerSecond(), 0.01f);
        m.reset();
        assertEquals(0f, m.percentPerSecond(), 0f);
    }
}
