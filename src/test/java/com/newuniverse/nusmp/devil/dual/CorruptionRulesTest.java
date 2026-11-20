package com.newuniverse.nusmp.devil.dual;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the creeping corruption of a devil's price (docs/devil_double_magic.md) must grow slowly, ease with kills and never make the screen unplayable; the curve is pure, so it is pinned here. */
class CorruptionRulesTest {
    private static final CorruptionRules.Params P = CorruptionRules.Params.DEFAULT;
    private static final long QUIET = 100_000L;

    private static float run(float from, int seconds, boolean assim, boolean union, long sinceKill) {
        float l = from;
        for (int i = 0; i < seconds; i++) l = CorruptionRules.step(l, true, assim, union, sinceKill, P);
        return l;
    }

    @Test
    void idleGrowthIsSlow() {
        float oneMinute = run(0f, 60, false, false, QUIET);
        assertEquals(P.idlePerMin(), oneMinute, 0.01f);
        assertTrue(run(0f, 60 * 60, false, false, QUIET) <= P.cap());
        assertTrue(oneMinute < 50f, "a minute of idleness is a small part of the screen");
    }

    @Test
    void overuseGrowsFasterThanIdlenessAndIsCapped() {
        float idle = run(0f, 600, false, false, QUIET), assim = run(0f, 600, true, false, QUIET), union = run(0f, 600, true, true, QUIET);
        assertTrue(assim > idle && union > assim);
        assertEquals(P.cap(), run(0f, 60 * 60 * 5, true, true, QUIET), 1e-3f);
    }

    @Test
    void aRecentKillStopsTheIdleGrowthButNotTheOveruse() {
        assertEquals(0f, CorruptionRules.step(0f, true, false, false, 100L, P));
        assertTrue(CorruptionRules.step(0f, true, true, false, 100L, P) > 0f);
        assertTrue(CorruptionRules.step(0f, true, false, false, P.quietSeconds() * 20L, P) > 0f);
    }

    @Test
    void killsEaseAndDeathHalves() {
        assertEquals(460f, CorruptionRules.onKill(500f, P), 1e-3f);
        assertEquals(0f, CorruptionRules.onKill(10f, P));
        assertEquals(400f, CorruptionRules.onDeath(800f, P), 1e-3f);
        assertEquals(500f, CorruptionRules.onKill(500f, new CorruptionRules.Params(false, 20, 40, 80, 40, 0.5f, 850, 60)), "off: a kill changes nothing");
    }

    @Test
    void noPriceOrTheSwitchOffFadesItAway() {
        assertEquals(480f, CorruptionRules.step(500f, false, true, true, QUIET, P), 1e-3f);
        assertEquals(480f, CorruptionRules.step(500f, true, true, true, 0L, new CorruptionRules.Params(false, 20, 40, 80, 40, 0.5f, 850, 60)), 1e-3f);
        assertEquals(0f, CorruptionRules.step(5f, false, false, false, QUIET, P));
    }

    @Test
    void aLoweredCapPullsTheLevelDown() {
        CorruptionRules.Params low = new CorruptionRules.Params(true, 20, 40, 80, 40, 0.5f, 300, 60);
        assertEquals(300f, CorruptionRules.step(800f, true, false, false, QUIET, low), 1e-3f);
    }

    @Test
    void badInputIsClamped() {
        assertEquals(0f, CorruptionRules.step(Float.NaN, true, false, false, QUIET, new CorruptionRules.Params(true, 0, 0, 0, 0, 0, 0, 0)));
        CorruptionRules.Params wild = new CorruptionRules.Params(true, 1e9f, 1e9f, 1e9f, -5f, 7f, 1e9f, -4).clamped();
        assertTrue(wild.cap() <= CorruptionRules.CAP_MAX && wild.deathKeep() <= 1f && wild.killRelief() >= 0f && wild.quietSeconds() >= 0);
        assertTrue(CorruptionRules.step(0f, true, true, true, QUIET, new CorruptionRules.Params(true, 1e9f, 1e9f, 1e9f, 0, 1, 1e9f, 0)) <= CorruptionRules.CAP_MAX);
    }

    @Test
    void theScreenNeverBecomesUnplayable() {
        float prevCover = 0f;
        for (int pct = 0; pct <= 100; pct++) {
            float cover = CorruptionRules.cover(pct, 1f);
            assertTrue(cover >= prevCover, "the cover only grows with the level");
            prevCover = cover;
            assertTrue(cover <= 0.9f + 1e-6f);
            assertTrue(CorruptionRules.clearRadius(cover) >= CorruptionRules.MIN_CLEAR - 1e-6f, "a clear window always stays at " + pct);
            for (int i = 0; i <= 10; i++) assertTrue(CorruptionRules.ringAlpha(i / 10f, cover) <= 0.97f + 1e-6f);
        }
        assertEquals(0f, CorruptionRules.cover(100, 0f), "the client strength 0 draws nothing");
        assertEquals(0f, CorruptionRules.ringAlpha(0f, 0.85f), 1e-6f, "next to the window the dark is nothing");
        assertEquals(85, CorruptionRules.percent(850f));
        assertEquals(100, CorruptionRules.percent(5000f));
        assertEquals(0, CorruptionRules.percent(-3f));
    }
}
