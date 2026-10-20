package com.newuniverse.nusmp.client.vupgrade.godb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The pure maths of the upgraded God melee weapons: tiers, the mesh switch, the vein wave, the orbit, the swing ghosts, the particle counts. */
class UpgradeMathTest {
    @Test
    void tiersFollowTheDistanceRules() {
        assertEquals(UpgradeMath.TIER_FULL, UpgradeMath.tier(0.0, 12f, 24f));
        assertEquals(UpgradeMath.TIER_FULL, UpgradeMath.tier(11.9 * 11.9, 12f, 24f));
        assertEquals(UpgradeMath.TIER_LOD, UpgradeMath.tier(12.0 * 12.0, 12f, 24f));          // from 12 blocks the level-of-detail mesh
        assertEquals(UpgradeMath.TIER_LOD, UpgradeMath.tier(23.9 * 23.9, 12f, 24f));
        assertEquals(UpgradeMath.TIER_FAR, UpgradeMath.tier(24.0 * 24.0, 12f, 24f));          // from 24 blocks no additive pass
        assertEquals(UpgradeMath.TIER_FAR, UpgradeMath.tier(1.0e6, 12f, 24f));
    }

    @Test
    void theSwitchFallsBackToTheOldMesh() {
        assertEquals("up/x", UpgradeMath.meshFor(true, true, true, "x", "up/x"));
        assertEquals("x", UpgradeMath.meshFor(false, true, true, "x", "up/x"), "visual.upgrade=false");
        assertEquals("x", UpgradeMath.meshFor(true, false, true, "x", "up/x"), "no table");
        assertEquals("x", UpgradeMath.meshFor(true, true, false, "x", "up/x"), "the upgraded mesh does not load");
    }

    @Test
    void theVeinWaveStaysInRangeAndTravels() {
        float lo = 1f, hi = 0f;
        for (int i = 0; i < 400; i++) {
            float w = UpgradeMath.wave(i * 0.5f, 31.7f, 0.3f, 40f, 0.15f, 1f);
            lo = Math.min(lo, w);
            hi = Math.max(hi, w);
        }
        assertTrue(lo >= 0.15f - 1e-4f && hi <= 1f + 1e-4f, "range " + lo + " .. " + hi);
        assertTrue(hi - lo > 0.8f, "the wave uses most of its range");
        // the crest moves up: the crest of time t is below the crest of time t + 10 for a positive speed? the phase is y * k - t * speed, so the crest moves to higher y
        int c0 = argmax(0f), c1 = argmax(30f);
        assertTrue(c1 > c0, "the crest travels up the weapon (" + c0 + " -> " + c1 + ")");
    }

    private static int argmax(float time) {
        int best = 0;
        float v = -1f;
        for (int i = 0; i < 100; i++) {
            float w = UpgradeMath.wave(i * 0.4f, time, 0.05f, 40f, 0f, 1f);
            if (w > v + 1e-6f) {
                v = w;
                best = i;
            }
        }
        return best;
    }

    @Test
    void theWaveTableMatchesTheWave() {
        float[] t = new float[128];
        UpgradeMath.waveTable(5f, 0.3f, 30f, 0.1f, 1f, 0f, 200f, t);
        for (float y = 0f; y <= 200f; y += 7.3f) {
            assertEquals(UpgradeMath.wave(y, 5f, 0.3f, 30f, 0.1f, 1f), UpgradeMath.lookup(t, y, 0f, 200f), 0.06f, "y " + y);
        }
        assertEquals(t[0], UpgradeMath.lookup(t, -50f, 0f, 200f));
        assertEquals(t[127], UpgradeMath.lookup(t, 900f, 0f, 200f));
    }

    @Test
    void anOrbitStaysOnItsCircle() {
        UpgradeSpec.Orbiter o = new UpgradeSpec.Orbiter(6, 10f, 0.8f, 0.04f, 0.1f, 2f, 0.05f, 0.3f, 0f, 1f);
        float[] out = new float[5];
        for (int i = 0; i < 6; i++) {
            for (float time = 0f; time < 200f; time += 13f) {
                UpgradeMath.orbit(o, i, time, 0f, 150f, out);
                assertEquals(10f, (float) Math.hypot(out[0], out[2]), 1e-3f, "radius");
                assertTrue(Math.abs(out[1] - 120f) <= 2.0001f, "height 0.8 of the axis +- the bob: " + out[1]);
            }
        }
        float[] a = new float[5], b = new float[5];
        UpgradeMath.orbit(o, 0, 0f, 0f, 150f, a);
        UpgradeMath.orbit(o, 3, 0f, 0f, 150f, b);
        assertEquals(a[0], -b[0], 1e-3f, "piece 3 of 6 is opposite piece 0");
    }

    @Test
    void swingGhostsFadeAndVanishOutsideTheSwing() {
        assertEquals(0, UpgradeMath.swingAlpha(0f, 200, 1, 3));
        assertEquals(0, UpgradeMath.swingAlpha(1f, 200, 1, 3));
        int a1 = UpgradeMath.swingAlpha(0.5f, 200, 1, 3), a3 = UpgradeMath.swingAlpha(0.5f, 200, 3, 3);
        assertTrue(a1 > a3 && a3 > 0, "the farther ghost is fainter: " + a1 + " " + a3);
        assertTrue(a1 <= 200);
        assertEquals(0, UpgradeMath.swingAlpha(0.5f, 200, 1, 0));
    }

    @Test
    void particleCountsFollowQualityAndDistance() {
        assertEquals(0, UpgradeMath.stochasticCount(0f, 0.3f));
        assertEquals(2, UpgradeMath.stochasticCount(2f, 0.99f));
        assertEquals(3, UpgradeMath.stochasticCount(2.4f, 0.1f));
        assertEquals(2, UpgradeMath.stochasticCount(2.4f, 0.9f));
        assertEquals(1f, UpgradeMath.qualityFactor("high"));
        assertTrue(UpgradeMath.qualityFactor("medium") < 1f && UpgradeMath.qualityFactor("low") < UpgradeMath.qualityFactor("medium"));
        assertEquals(1f, UpgradeMath.qualityFactor(null));
        assertEquals(1f, UpgradeMath.distanceFactor(5.0, 12f, 32f));
        assertEquals(0.5f, UpgradeMath.distanceFactor(20.0, 12f, 32f));
        assertEquals(0f, UpgradeMath.distanceFactor(33.0, 12f, 32f));
    }

    @Test
    void haloAndFlickerStayBounded() {
        for (float t = 0f; t < 500f; t += 17f) {
            int a = UpgradeMath.haloAlpha(64, t, 0.05f, 0.3f);
            assertTrue(a >= 0 && a <= 255);
            float f = UpgradeMath.flicker(t, 0.1f, 0.37f);
            assertTrue(f > 0.85f && f < 1.15f, "flicker " + f);
            float s = UpgradeMath.haloScale(1.07f, t, 0.03f, 0.05f);
            assertTrue(s > 1.0f && s < 1.12f);
        }
        assertEquals(1f, UpgradeMath.flicker(10f, 0f, 0.37f));
        assertEquals(0xFFFFFF, UpgradeMath.argb(0.5f) & 0xFFFFFF);
        assertEquals(255, UpgradeMath.argb(2f) >>> 24);
        assertEquals(0, UpgradeMath.argb(-1f) >>> 24);
    }
}
