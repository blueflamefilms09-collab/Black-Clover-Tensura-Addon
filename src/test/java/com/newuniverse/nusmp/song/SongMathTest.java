package com.newuniverse.nusmp.song;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The voice arithmetic: RMS, gate, smoothing, hold, the strength table and the pulse limits. */
class SongMathTest {
    @Test
    void silenceIsZero() {
        assertEquals(0f, SongMath.rms(new short[960]), 1e-6f);
        assertEquals(0f, SongMath.rms(new short[0]), 1e-6f);
        assertEquals(0f, SongMath.rms(null), 1e-6f);
    }

    @Test
    void fullScaleSquareWaveIsOne() {
        short[] pcm = new short[480];
        for (int i = 0; i < pcm.length; i++) pcm[i] = (i % 2 == 0) ? Short.MAX_VALUE : Short.MIN_VALUE;
        assertEquals(1f, SongMath.rms(pcm), 1e-3f);
    }

    @Test
    void halfScaleIsAboutHalf() {
        short[] pcm = new short[480];
        for (int i = 0; i < pcm.length; i++) pcm[i] = (i % 2 == 0) ? (short) 16384 : (short) -16384;
        assertEquals(0.5f, SongMath.rms(pcm), 1e-3f);
    }

    @Test
    void gateCutsQuietAndScalesLoud() {
        assertEquals(0f, SongMath.loudness(0.05f, 0.08f, 0.35f));
        assertEquals(0f, SongMath.loudness(0.08f, 0.08f, 0.35f), "exactly at the gate is still silence");
        assertEquals(0.5f, SongMath.loudness(0.215f, 0.08f, 0.35f), 1e-4f);
        assertEquals(1f, SongMath.loudness(0.9f, 0.08f, 0.35f));
    }

    @Test
    void smoothingRisesFastAndFallsSlow() {
        float up = SongMath.smooth(0f, 1f, 0.6f, 0.15f);
        float down = SongMath.smooth(1f, 0f, 0.6f, 0.15f);
        assertEquals(0.6f, up, 1e-6f);
        assertEquals(0.85f, down, 1e-6f);
    }

    @Test
    void strengthRunsFromThreeTenthsToOne() {
        assertEquals(0.3f, SongMath.strength(0f), 1e-6f);
        assertEquals(1.0f, SongMath.strength(1f), 1e-6f);
        assertEquals(1.0f, SongMath.strength(7f), 1e-6f);
        assertEquals(0.3f, SongMath.strength(-2f), 1e-6f);
    }

    @Test
    void meterHoldsThroughShortPausesThenFalls() {
        SingingMeter m = new SingingMeter();
        for (int t = 0; t < 10; t++) m.feed(1f, t * 20L, 400);
        float loud = m.level(200, 400);
        assertTrue(loud > 0.9f);
        // a pause shorter than the hold: still sung
        assertEquals(loud, m.level(200 + 300, 400), 1e-6f);
        // past the hold plus the tail: nothing, even if no further frame arrived
        assertEquals(0f, m.level(200 + 400 + 300 + 1, 400), 1e-6f);
        // in the tail it is falling
        float tail = m.level(200 + 400 + 150, 400);
        assertTrue(tail > 0f && tail < loud);
    }

    @Test
    void silentFramesAfterTheHoldBringTheLevelDown() {
        SingingMeter m = new SingingMeter();
        m.feed(1f, 0, 400);
        long t = 0;
        for (int i = 0; i < 80; i++) { t += 20; m.feed(0f, t, 400); }
        assertEquals(0f, m.level(t, 400), 1e-6f);
    }

    @Test
    void forcedLevelHoldsOnlyWhileRefreshed() {
        SingingMeter m = new SingingMeter();
        m.force(0.6f, 1000);
        assertEquals(0.6f, m.level(1200, 400), 1e-6f);
        assertEquals(0f, m.level(5000, 400), 1e-6f, "a lost key-up cannot sing forever");
    }

    @Test
    void pulsesAreRateLimited() {
        assertTrue(PulseScheduler.due(100, -1, 40), "never pulsed: due");
        assertFalse(PulseScheduler.due(100, 80, 40));
        assertFalse(PulseScheduler.due(119, 80, 40));
        assertTrue(PulseScheduler.due(120, 80, 40));
    }

    @Test
    void perTickBudgetCapsAndResets() {
        PulseScheduler s = new PulseScheduler();
        assertTrue(s.tryTake(5, 2));
        assertTrue(s.tryTake(5, 2));
        assertFalse(s.tryTake(5, 2));
        assertTrue(s.tryTake(6, 2), "a new tick gets a new budget");
    }

    @Test
    void modeTable() {
        assertEquals(SongMode.HEAL, SongMode.byId(1));
        assertEquals(SongMode.NONE, SongMode.byId(99));
        assertEquals(1, SongMode.skillLevel(0.0));
        assertEquals(5, SongMode.skillLevel(1.0));
        assertEquals(0.02f, SongMode.healFraction(1, 1f), 1e-6f);
        assertEquals(0.10f, SongMode.healFraction(5, 1f), 1e-6f);
        assertEquals(60, SongMode.sleepTicks(1f));
        assertEquals(18, SongMode.sleepTicks(0.3f));
        assertEquals(3.0, SongMode.knockDistance(1f), 1e-9);
        assertTrue(SongMode.REQUIEM.pulseCostPercent(false) > SongMode.HEAL.pulseCostPercent(false), "requiem is the mana-hungry one");
        assertEquals(3 * SongMode.BARRIER.pulseCostPercent(false), SongMode.BARRIER.pulseCostPercent(true), 1e-9);
        assertTrue(SongMode.requiemDamage(5, 1f) <= 3.5f, "weak on purpose");
    }

    @Test
    void crescendoChargesReleasesAndForgives() {
        CrescendoCharge c = new CrescendoCharge();
        for (int i = 0; i < 59; i++) assertEquals(CrescendoCharge.Result.CHARGING, c.tick(0.8f, 0.6f, 60));
        // letting go one tick early drains, it does not fire
        assertEquals(CrescendoCharge.Result.CHARGING, c.tick(0.0f, 0.6f, 60));
        for (int i = 0; i < 3; i++) assertEquals(CrescendoCharge.Result.CHARGING, c.tick(0.9f, 0.6f, 60));
        assertEquals(60, c.charge());
        assertEquals(0.9f, c.peak(), 1e-6f);
        assertEquals(CrescendoCharge.Result.RELEASE, c.tick(0.1f, 0.6f, 60));
        c.reset();
        assertEquals(0, c.charge());
        assertEquals(CrescendoCharge.Result.IDLE, c.tick(0.1f, 0.6f, 60));
    }

    @Test
    void crescendoQuietStopLosesTheChargeFast() {
        CrescendoCharge c = new CrescendoCharge();
        for (int i = 0; i < 10; i++) c.tick(0.8f, 0.6f, 60);
        for (int i = 0; i < 5; i++) c.tick(0f, 0.6f, 60);
        assertEquals(0, c.charge());
    }
}
