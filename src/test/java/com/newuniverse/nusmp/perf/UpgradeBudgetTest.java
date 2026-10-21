package com.newuniverse.nusmp.perf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 1.110.0: the budget maths of the upgrade effects (the pure class behind {@code client/upgrade/UpgradeGate}): caps, scales, buckets, the live cap, bursts. */
class UpgradeBudgetTest {
    @Test
    void theCapsFallWithTheQualityAndTheOwnersDefaultsAreHalfOrBetter() {
        assertTrue(UpgradeBudget.perItemPerSecond(VfxQuality.LOW) < UpgradeBudget.perItemPerSecond(VfxQuality.MEDIUM));
        assertTrue(UpgradeBudget.perItemPerSecond(VfxQuality.MEDIUM) < UpgradeBudget.perItemPerSecond(VfxQuality.HIGH));
        assertTrue(UpgradeBudget.perItemPerSecond(VfxQuality.HIGH) <= 16, "an item may spawn at most 16 a second");
        for (VfxQuality q : VfxQuality.values()) assertTrue(UpgradeBudget.perPlayerPerSecond(q) >= UpgradeBudget.perItemPerSecond(q), q + ": a player may spawn at least what one item may");
        assertEquals(0.5f, UpgradeBudget.DEFAULT_DENSITY, 0f);
        assertEquals(0.5f, UpgradeBudget.density(Float.NaN), 0f);
        assertEquals(0f, UpgradeBudget.density(-3f), 0f);
        assertEquals(1f, UpgradeBudget.density(9f), 0f);
    }

    @Test
    void theLiveCapIsTiedToTheQualityAndToTheClientAllowance() {
        assertEquals(240, UpgradeBudget.liveCap(VfxQuality.HIGH, 400));
        assertEquals(140, UpgradeBudget.liveCap(VfxQuality.MEDIUM, 400));
        assertEquals(60, UpgradeBudget.liveCap(VfxQuality.LOW, 400));
        assertEquals(60, UpgradeBudget.liveCap(VfxQuality.HIGH, 100), "a quarter of the allowance: a quarter of the cap");
        assertEquals(60, UpgradeBudget.liveCap(VfxQuality.HIGH, 1), "never under a quarter of the base");
        assertEquals(360, UpgradeBudget.liveCap(VfxQuality.HIGH, 100000), "never over one and a half of the base");
        assertEquals(240, UpgradeBudget.liveCap(VfxQuality.HIGH, 0), "an unlimited allowance keeps the base cap");
    }

    @Test
    void nothingSpawnsBelowTheMinimumFrameRateAndItIsFullFromFortyFive() {
        assertEquals(1f, UpgradeBudget.fpsFactor(0), 0f, "not measured yet");
        assertEquals(1f, UpgradeBudget.fpsFactor(60), 0f);
        assertEquals(1f, UpgradeBudget.fpsFactor(UpgradeBudget.FULL_FPS), 0f);
        assertEquals(0f, UpgradeBudget.fpsFactor(UpgradeBudget.MIN_FPS - 1), 0f);
        float last = 0f;
        for (int fps = UpgradeBudget.MIN_FPS; fps <= UpgradeBudget.FULL_FPS; fps++) {
            float f = UpgradeBudget.fpsFactor(fps);
            assertTrue(f >= last, "monotonic at " + fps);
            last = f;
        }
    }

    @Test
    void anotherPlayersItemSpawnsNothingBeyondSixteenBlocksAndNothingBehindTheCamera() {
        assertEquals(1f, UpgradeBudget.distanceFactor(100.0, true), 0f, "your own item is never distance-cut");
        assertEquals(1f, UpgradeBudget.distanceFactor(8.0, false), 0f);
        assertEquals(0.5f, UpgradeBudget.distanceFactor(12.0, false), 1e-6f);
        assertEquals(0f, UpgradeBudget.distanceFactor(16.0, false), 0f);
        assertEquals(0f, UpgradeBudget.distanceFactor(40.0, false), 0f);
        assertEquals(1f, UpgradeBudget.viewFactor(1.0, 100.0, false), 0f);
        assertEquals(0f, UpgradeBudget.viewFactor(-0.5, 100.0, false), 0f, "behind the camera");
        assertEquals(0f, UpgradeBudget.viewFactor(-0.1, 100.0, false), 0f);
        assertEquals(0.5f, UpgradeBudget.viewFactor(0.1, 100.0, false), 1e-6f);
        assertEquals(1f, UpgradeBudget.viewFactor(-1.0, 4.0, false), 0f, "somebody within three blocks is always shown");
        assertEquals(1f, UpgradeBudget.viewFactor(-1.0, 100.0, true), 0f, "your own item is not view-culled");
    }

    @Test
    void theScaleIsTheProductOfDensityQualityFpsDistanceAndView() {
        assertEquals(0.5f, UpgradeBudget.scale(0.5f, VfxQuality.HIGH, 60, 3.0, 1.0, true), 1e-6f, "the default density halves the table rates");
        assertEquals(1f, UpgradeBudget.scale(1f, VfxQuality.HIGH, 60, 3.0, 1.0, true), 1e-6f);
        assertEquals(0.25f, UpgradeBudget.scale(0.5f, VfxQuality.LOW, 60, 3.0, 1.0, true), 1e-6f);
        assertEquals(0f, UpgradeBudget.scale(0f, VfxQuality.HIGH, 60, 3.0, 1.0, true), 0f);
        assertEquals(0f, UpgradeBudget.scale(1f, VfxQuality.HIGH, 5, 3.0, 1.0, true), 0f, "a slideshow gets nothing");
        assertEquals(0f, UpgradeBudget.scale(1f, VfxQuality.HIGH, 60, 20.0, 1.0, false), 0f, "beyond 16 blocks");
        assertEquals(0f, UpgradeBudget.scale(1f, VfxQuality.HIGH, 60, 10.0, -1.0, false), 0f, "behind the camera");
        for (float d : new float[]{0f, 0.1f, 0.5f, 1f}) for (VfxQuality q : VfxQuality.values()) assertTrue(UpgradeBudget.scale(d, q, 60, 5.0, 1.0, false) <= d + 1e-6f, "never above the density");
    }

    @Test
    void stochasticRoundingKeepsTheMean() {
        assertEquals(0, UpgradeBudget.stochastic(0f, 0.3f));
        assertEquals(0, UpgradeBudget.stochastic(-1f, 0.3f));
        assertEquals(2, UpgradeBudget.stochastic(2f, 0.99f));
        assertEquals(1, UpgradeBudget.stochastic(0.4f, 0.1f));
        assertEquals(0, UpgradeBudget.stochastic(0.4f, 0.5f));
        double sum = 0;
        int n = 10000;
        for (int i = 0; i < n; i++) sum += UpgradeBudget.stochastic(1.3f, (i + 0.5f) / n);
        assertEquals(1.3, sum / n, 0.01);
    }

    @Test
    void anItemBucketHoldsTheItemToItsRateAndRefillsOverTime() {
        UpgradeBudget b = new UpgradeBudget();
        VfxQuality q = VfxQuality.HIGH;
        long tick = 1000;
        int spent = 0;
        for (int t = 0; t < 100; t++) {                                                  // five seconds of an item that wants 50 a tick
            spent += b.take(7, 1, tick + t, 50, q, 400);
        }
        int max = UpgradeBudget.perItemPerSecond(q) * 5 + (int) Math.ceil(UpgradeBudget.perItemPerSecond(q) * 0.5) + 1;
        assertTrue(spent <= max, "an item spent " + spent + " in five seconds, the cap allows about " + max);
        assertTrue(spent >= UpgradeBudget.perItemPerSecond(q) * 4, "but it does get its rate: " + spent);
    }

    @Test
    void aPlayerBucketHoldsAPlayerWhoHoldsManyItems() {
        UpgradeBudget b = new UpgradeBudget();
        VfxQuality q = VfxQuality.HIGH;
        int spent = 0;
        for (int t = 0; t < 100; t++) for (int item = 0; item < 8; item++) spent += b.take(3, item, 2000 + t, 50, q, 400);
        int max = UpgradeBudget.perPlayerPerSecond(q) * 5 + UpgradeBudget.perPlayerPerSecond(q) / 2 + 2;
        assertTrue(spent <= max, "a player spent " + spent + " with eight items, the cap allows about " + max);
    }

    @Test
    void theGlobalLiveCapHoldsACrowd() {
        UpgradeBudget b = new UpgradeBudget();
        VfxQuality q = VfxQuality.HIGH;
        int cap = UpgradeBudget.liveCap(q, 400);
        int maxLive = 0;
        for (long t = 0; t < 400; t++) {
            for (int holder = 0; holder < 40; holder++) b.take(holder, 1, 5000 + t, 20, q, 400);
            maxLive = Math.max(maxLive, b.live(5000 + t));
        }
        assertTrue(maxLive <= cap, "live peaked at " + maxLive + " over the cap " + cap);
        assertTrue(maxLive >= cap / 2, "and a crowd does fill it: " + maxLive);
        assertTrue(b.granted() < b.asked() / 10, "most of what forty greedy holders asked was refused");
    }

    @Test
    void liveParticlesExpireAndTheStateIsPruned() {
        UpgradeBudget b = new UpgradeBudget();
        assertEquals(3, b.take(1, 1, 100, 3, VfxQuality.HIGH, 400));
        assertEquals(3, b.live(100));
        assertEquals(3, b.live(100 + UpgradeBudget.LIFE_TICKS - 1));
        assertEquals(0, b.live(100 + UpgradeBudget.LIFE_TICKS));
        for (int h = 0; h < 50; h++) b.take(h, 1, 200, 1, VfxQuality.HIGH, 400);
        assertTrue(b.trackedPlayers() >= 50);
        b.take(1, 1, 200 + 400, 1, VfxQuality.HIGH, 400);
        assertTrue(b.trackedPlayers() <= 2, "holders that left are forgotten: " + b.trackedPlayers());
        b.clear();
        assertEquals(0, b.trackedItems());
        assertEquals(0, b.live(200));
    }

    @Test
    void aClockThatGoesBackDoesNotBreakTheBuckets() {
        UpgradeBudget b = new UpgradeBudget();
        b.take(1, 1, 5000, 4, VfxQuality.HIGH, 400);
        assertTrue(b.take(1, 1, 10, 4, VfxQuality.HIGH, 400) >= 0);
        assertTrue(b.take(1, 1, 11, 4, VfxQuality.HIGH, 400) >= 0);
    }

    @Test
    void burstsAreScaledCappedAndCountedAsLive() {
        assertEquals(0, UpgradeBudget.burstCount(40, 0f, VfxQuality.HIGH, 60));
        assertEquals(0, UpgradeBudget.burstCount(40, 0.5f, VfxQuality.HIGH, 5));
        assertEquals(20, UpgradeBudget.burstCount(40, 0.5f, VfxQuality.HIGH, 60));
        assertEquals(UpgradeBudget.burstCap(VfxQuality.HIGH), UpgradeBudget.burstCount(400, 1f, VfxQuality.HIGH, 60));
        assertEquals(1, UpgradeBudget.burstCount(1, 0.1f, VfxQuality.LOW, 60), "something is wanted, so at least one");
        assertTrue(UpgradeBudget.burstCap(VfxQuality.LOW) < UpgradeBudget.burstCap(VfxQuality.MEDIUM) && UpgradeBudget.burstCap(VfxQuality.MEDIUM) < UpgradeBudget.burstCap(VfxQuality.HIGH));
        UpgradeBudget b = new UpgradeBudget();
        int cap = UpgradeBudget.liveCap(VfxQuality.LOW, 400);
        assertEquals(cap, b.takeBurst(10, 1000, VfxQuality.LOW, 400));
        assertEquals(0, b.takeBurst(10, 5, VfxQuality.LOW, 400), "the cap is spent");
        assertEquals(0, b.take(1, 1, 10, 5, VfxQuality.LOW, 400), "and an ambient layer gets nothing either");
    }

    @Test
    void theReportLineSaysWhatItShould() {
        UpgradeBudget b = new UpgradeBudget();
        b.take(1, 1, 10, 3, VfxQuality.HIGH, 400);
        String line = b.line(10, VfxQuality.HIGH, 400);
        assertTrue(line.contains("live 3/240") && line.contains("granted 3 of 3"), line);
    }
}
