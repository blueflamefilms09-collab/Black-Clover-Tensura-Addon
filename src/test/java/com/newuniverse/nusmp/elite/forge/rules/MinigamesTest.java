package com.newuniverse.nusmp.elite.forge.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the four minigames are the part the server must trust nothing about, so their rules are pure and tested to the number: a perfect play scores (nearly) the
 * maximum, doing nothing or playing at random scores far less, no input can score above 1000, an input of the wrong shape is refused and scores 0, a game is
 * deterministic in its seed, and every game claims at least the time it needs.
 */
class MinigamesTest {
    static final long[] SEEDS = {1L, 42L, -7L, 0x1234_5678_9ABCL, Long.MAX_VALUE, 987654321L, 31337L, -1L};

    private static int minPerfect(MinigameType t) {
        return switch (t) {
            case HAMMER_TIMING, RUNE_ALIGNMENT -> 990;
            case MAGICULE_FLOW -> 930;
            case TEMPERATURE_FORGE -> 900;
        };
    }

    @Test
    void perfectInputIsValidAndScoresHigh() {
        for (MinigameType t : MinigameType.values()) {
            Minigame g = Minigames.of(t);
            for (int d = 1; d <= 5; d++) {
                for (long seed : SEEDS) {
                    int[] in = g.perfectInput(seed, d);
                    assertNull(g.validate(seed, d, in), t + " d" + d + " seed " + seed + " perfect input must be valid");
                    int s = g.score(seed, d, in);
                    assertTrue(s >= minPerfect(t), t + " d" + d + " seed " + seed + " perfect scored only " + s);
                    assertTrue(s <= Minigame.MAX_SCORE, "score above the maximum");
                    assertTrue(in.length <= g.maxInputLength(d), t + " perfect input longer than the payload cap");
                    assertTrue(g.claimedMillis(seed, d, in) <= g.limitMillis(d), t + " claims more than the step's limit");
                }
            }
        }
    }

    @Test
    void doingNothingOrRandomPlayScoresLow() {
        for (MinigameType t : MinigameType.values()) {
            Minigame g = Minigames.of(t);
            for (int d = 1; d <= 5; d++) {
                long worst = 0;
                double mean = 0;
                int n = 0;
                for (long seed : SEEDS) {
                    int[] idle = switch (t) {
                        case HAMMER_TIMING -> new int[0];
                        case MAGICULE_FLOW -> filled(MagiculeFlow.samples(d), 500);
                        case TEMPERATURE_FORGE -> filled(TemperatureForge.samples(d), 0);
                        case RUNE_ALIGNMENT -> new int[2 * RuneAlignment.rings(d)];
                    };
                    assertNull(g.validate(seed, d, idle), t + " idle input should be valid");
                    int s = g.score(seed, d, idle);
                    worst = Math.max(worst, s);
                    n++;
                    mean += s;
                    assertTrue(s <= 900, t + " d" + d + " seed " + seed + ": doing nothing scored " + s);
                }
                assertTrue(n > 0);
                assertTrue(mean / n <= 700, t + " d" + d + ": doing nothing averages " + mean / n);
                if (t == MinigameType.HAMMER_TIMING || t == MinigameType.RUNE_ALIGNMENT) assertEquals(0L, worst, t + " idle must be 0");
            }
        }
    }

    @Test
    void randomInputScoresFarBelowPerfect() {
        java.util.Random rnd = new java.util.Random(5);
        for (int d = 1; d <= 5; d++) {
            double hammer = 0, flow = 0, temp = 0, rune = 0;
            int trials = 40;
            for (int k = 0; k < trials; k++) {
                long seed = rnd.nextLong();
                int[] h = new int[HammerTiming.strikes(d)];
                int t = 250;
                for (int i = 0; i < h.length; i++) { t += 180 + rnd.nextInt(1200); h[i] = t; }
                hammer += Minigames.HAMMER.score(seed, d, h);
                int[] f = new int[MagiculeFlow.samples(d)];
                for (int i = 0; i < f.length; i++) f[i] = rnd.nextInt(1001);
                flow += Minigames.FLOW.score(seed, d, f);
                int[] c = new int[TemperatureForge.samples(d)];
                for (int i = 0; i < c.length; i++) c[i] = rnd.nextInt(2);
                temp += Minigames.TEMPERATURE.score(seed, d, c);
                int[] r = new int[2 * RuneAlignment.rings(d)];
                for (int i = 0; i < r.length / 2; i++) { r[2 * i] = rnd.nextInt(3600); r[2 * i + 1] = 600 + 500 * i; }
                rune += Minigames.RUNES.score(seed, d, r);
            }
            assertTrue(hammer / trials < 500, "random hammer d" + d + " avg " + hammer / trials);
            assertTrue(flow / trials < 600, "random flow d" + d + " avg " + flow / trials);
            assertTrue(temp / trials < 600, "random temperature d" + d + " avg " + temp / trials);
            assertTrue(rune / trials < 300, "random runes d" + d + " avg " + rune / trials);
        }
    }

    @Test
    void shapeErrorsAreRefusedAndScoreZero() {
        long seed = 99L;
        int d = 3;
        // hammer: too many strikes, too early, too close, past the limit
        int n = HammerTiming.strikes(d);
        assertNotNull(Minigames.HAMMER.validate(seed, d, new int[n + 1]));
        assertNotNull(Minigames.HAMMER.validate(seed, d, new int[]{100}));
        assertNotNull(Minigames.HAMMER.validate(seed, d, new int[]{500, 600}));
        assertNotNull(Minigames.HAMMER.validate(seed, d, new int[]{500, Minigames.HAMMER.limitMillis(d) + 1}));
        assertEquals(0, Minigames.HAMMER.score(seed, d, new int[]{500, 600}));
        // flow: wrong length, out of range
        assertNotNull(Minigames.FLOW.validate(seed, d, new int[5]));
        int[] bad = filled(MagiculeFlow.samples(d), 500);
        bad[3] = 1001;
        assertNotNull(Minigames.FLOW.validate(seed, d, bad));
        assertEquals(0, Minigames.FLOW.score(seed, d, bad));
        bad[3] = -1;
        assertNotNull(Minigames.FLOW.validate(seed, d, bad));
        // temperature: values other than 0/1
        int[] t = filled(TemperatureForge.samples(d), 0);
        t[10] = 2;
        assertNotNull(Minigames.TEMPERATURE.validate(seed, d, t));
        assertEquals(0, Minigames.TEMPERATURE.score(seed, d, t));
        // runes: angle out of range, lock too soon, a lock after an unlocked ring, wrong length
        int rings = RuneAlignment.rings(d);
        assertNotNull(Minigames.RUNES.validate(seed, d, new int[2 * rings - 1]));
        int[] r = Minigames.RUNES.perfectInput(seed, d);
        int[] r1 = r.clone();
        r1[0] = 3600;
        assertNotNull(Minigames.RUNES.validate(seed, d, r1));
        int[] r2 = r.clone();
        r2[1] = 300;
        assertNotNull(Minigames.RUNES.validate(seed, d, r2));
        int[] r3 = r.clone();
        r3[1] = 0;
        assertNotNull(Minigames.RUNES.validate(seed, d, r3));
        int[] r4 = r.clone();
        r4[3] = r4[1] + 100;
        assertNotNull(Minigames.RUNES.validate(seed, d, r4));
        assertNotNull(Minigames.RUNES.validate(seed, d, null));
    }

    @Test
    void gamesAreDeterministicInTheSeed() {
        for (MinigameType t : MinigameType.values()) {
            Minigame g = Minigames.of(t);
            int[] a = g.perfectInput(77L, 3), b = g.perfectInput(77L, 3), c = g.perfectInput(78L, 3);
            assertEquals(java.util.Arrays.toString(a), java.util.Arrays.toString(b), t + " same seed must give the same play");
            assertEquals(g.score(77L, 3, a), g.score(77L, 3, a));
            // another seed means another game: the first game's perfect play must not be perfect in it (except by chance, which these seeds avoid)
            if (t != MinigameType.TEMPERATURE_FORGE) assertTrue(g.score(78L, 3, a) < g.score(77L, 3, a), t + " a play must not carry over to another seed");
            assertNotNull(c);
        }
    }

    @Test
    void temperatureAndFlowApplyTheirPhysicalLimits() {
        int d = 3;
        long seed = 5L;
        // a button flipped on EVERY tick is coerced: the state never changes twice within MIN_RUN ticks
        int n = TemperatureForge.samples(d);
        TemperatureForge.Run a = new TemperatureForge.Run(seed, d);
        boolean last = false;
        int run = 0, shortest = Integer.MAX_VALUE, flips = 0;
        for (int i = 0; i < n; i++) {
            a.step(i % 2);
            if (i > 0 && a.heating() != last) {
                if (flips > 0) shortest = Math.min(shortest, run);
                flips++;
                run = 0;
            }
            run++;
            last = a.heating();
        }
        assertTrue(flips > 5, "the coerced state still flips now and then: " + flips);
        assertTrue(shortest >= TemperatureForge.MIN_RUN, "a flip came after " + shortest + " ticks");
        assertTrue(flips < n / 2, "flipping every tick was not limited: " + flips);
        // flow: a jump from one end of the meter to the other changes the push by at most the slew per tick
        MagiculeFlow.Run r1 = new MagiculeFlow.Run(seed, d), r2 = new MagiculeFlow.Run(seed, d);
        r1.step(0);
        r2.step(500);
        assertTrue(Math.abs(r1.needle() - r2.needle()) <= MagiculeFlow.AUTHORITY * MagiculeFlow.SLEW + 1e-9, "the push is slew-limited");
    }

    private static int[] filled(int n, int v) {
        int[] a = new int[n];
        java.util.Arrays.fill(a, v);
        return a;
    }
}
