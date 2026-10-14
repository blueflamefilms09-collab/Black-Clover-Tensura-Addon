package com.newuniverse.nusmp.boss.vfx;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the boss move effects: the telegraph / strike bit of the seed, the lifetimes, the radius clamp and the per-tick budget. */
class BossVfxRulesTest {
    @Test
    void theLowestBitOfTheSeedSaysStrikeOrTelegraphWhateverTheRandomBits() {
        Random r = new Random(7);
        for (int i = 0; i < 2000; i++) {
            long raw = r.nextLong();
            assertTrue(BossVfxRules.isStrike(BossVfxRules.seed(raw, true)));
            assertFalse(BossVfxRules.isStrike(BossVfxRules.seed(raw, false)));
            assertEquals(raw >>> 1, BossVfxRules.seed(raw, true) >>> 1, "the other 63 bits are kept");
            assertEquals(raw >>> 1, BossVfxRules.seed(raw, false) >>> 1);
        }
    }

    @Test
    void aTelegraphLastsAsLongAsItsWindUpWithinBounds() {
        assertEquals(6, BossVfxRules.telegraphTicks(0));
        assertEquals(6, BossVfxRules.telegraphTicks(-10));
        assertEquals(24, BossVfxRules.telegraphTicks(24));
        assertEquals(BossVfxRules.MAX_TELEGRAPH_TICKS, BossVfxRules.telegraphTicks(5000));
        for (int w = 1; w < 400; w++) {
            int t = BossVfxRules.telegraphTicks(w);
            assertTrue(t >= 6 && t <= BossVfxRules.MAX_TELEGRAPH_TICKS);
            assertTrue(t <= Math.max(6, w), "never longer than the wind-up it announces");
        }
    }

    @Test
    void aStrikeIsShortAndGrowsALittleWithItsSize() {
        assertEquals(12, BossVfxRules.strikeTicks(0));
        assertEquals(40, BossVfxRules.strikeTicks(1000));
        double prev = 0;
        for (double size = 0; size < 60; size += 0.5) {
            int t = BossVfxRules.strikeTicks(size);
            assertTrue(t >= 10 && t <= 40 && t >= prev, "monotone and bounded at " + size);
            prev = t;
        }
    }

    @Test
    void aRadiusIsClampedToWhatARoomCanHold() {
        assertEquals(0.5f, BossVfxRules.clampRadius(-3));
        assertEquals(0.5f, BossVfxRules.clampRadius(Double.NEGATIVE_INFINITY));
        assertEquals(36f, BossVfxRules.clampRadius(500));
        assertEquals(7.5f, BossVfxRules.clampRadius(7.5));
    }

    @Test
    void theThrottleSpendsTwelveATickAndStartsOverEachTick() {
        BossVfxRules.Throttle t = new BossVfxRules.Throttle();
        for (long tick = 100; tick < 110; tick++) {
            int ok = 0;
            for (int i = 0; i < 50; i++) if (t.allow(tick)) ok++;
            assertEquals(BossVfxRules.PER_TICK, ok, "exactly the budget in tick " + tick);
            assertEquals(BossVfxRules.PER_TICK, t.spent());
        }
        assertTrue(t.allow(500), "a new tick has a fresh budget");
        assertEquals(1, t.spent());
        assertTrue(BossVfxRules.PER_TICK * 5 < 64 || BossVfxRules.PER_BOSS_ACTIVE < 64, "a boss never fills the client's 64 slots alone");
    }
}
