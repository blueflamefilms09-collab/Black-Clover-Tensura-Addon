package com.newuniverse.nusmp.meteor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure rules of the shared meteor: the fall is bounded, monotone and lands exactly on the impact; the caps and the clamps hold. */
class MeteorRulesTest {
    private static double dist(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
    }

    @Test
    void everyFallLengthStartsInsideTheHeightAndSpeedBounds() {
        for (int t = -5; t <= 200; t++) {
            int f = MeteorRules.clampFall(t);
            assertTrue(f >= MeteorRules.MIN_FALL && f <= MeteorRules.MAX_FALL);
            double h = MeteorRules.height(t);
            assertTrue(h <= MeteorRules.MAX_HEIGHT && h >= 1.0, "height " + h);
            assertTrue(h / f <= MeteorRules.MAX_SPEED + 1e-9, "average speed at " + t);
            if (f >= 19) assertTrue(h >= MeteorRules.LONG_MIN_HEIGHT);
        }
    }

    @Test
    void theFallComesCloserEveryTickAndLandsExactly() {
        double[] impact = {10, 64, -3};
        for (int total : new int[]{MeteorRules.MIN_FALL, 14, 30, MeteorRules.MAX_FALL}) {
            double[] start = MeteorRules.start(impact, total, 1.234);
            double last = Double.MAX_VALUE;
            for (int t = 0; t <= total; t++) {
                double d = dist(MeteorRules.position(start, impact, t, total), impact);
                assertTrue(d < last || (t == 0), "tick " + t + " of " + total);
                last = d;
            }
            assertEquals(0.0, last, 1e-9);
            assertEquals(dist(start, impact), dist(MeteorRules.position(start, impact, 0, total), impact), 1e-9);
        }
    }

    @Test
    void theStartIsAboveAndToTheSide() {
        double[] s = MeteorRules.start(new double[]{0, 0, 0}, 20, 0.0);
        assertTrue(s[1] >= MeteorRules.LONG_MIN_HEIGHT);
        assertEquals(MeteorRules.sideOffset(MeteorRules.height(20)), s[0], 1e-9);
        assertEquals(0.0, s[2], 1e-9);
    }

    @Test
    void capsAreEnforcedAndClampedToTheAbsoluteMaxima() {
        assertTrue(MeteorRules.canLaunch(0, 0, 4, 24));
        assertFalse(MeteorRules.canLaunch(4, 4, 4, 24));
        assertFalse(MeteorRules.canLaunch(0, 24, 4, 24));
        assertFalse(MeteorRules.canLaunch(MeteorRules.ABS_MAX_PER_CASTER, 0, 1000, 1000));
        assertFalse(MeteorRules.canLaunch(0, MeteorRules.ABS_MAX_GLOBAL, 1000, 1000));
        assertFalse(MeteorRules.canLaunch(0, 0, 0, 0) && MeteorRules.cap(0, 1, 8) < 1);
    }

    @Test
    void clampsHandleNaNAndHugeValues() {
        assertEquals(1f, MeteorRules.clampSize(Float.NaN));
        assertEquals(MeteorRules.MAX_SIZE, MeteorRules.clampSize(1e9f));
        assertEquals(MeteorRules.MIN_RADIUS, MeteorRules.clampRadius(Double.NaN));
        assertEquals(MeteorRules.MAX_RADIUS, MeteorRules.clampRadius(1e9));
        assertEquals(0f, MeteorRules.clampDamage(Float.NaN));
        assertEquals(MeteorRules.MAX_DAMAGE, MeteorRules.clampDamage(Float.POSITIVE_INFINITY));
        assertEquals(0.0, MeteorRules.clampKnockback(-4));
    }

    @Test
    void expiryAndFalloff() {
        assertFalse(MeteorRules.expired(30 + MeteorRules.GRACE, 30));
        assertTrue(MeteorRules.expired(30 + MeteorRules.GRACE + 1, 30));
        assertEquals(1.0, MeteorRules.falloff(0, 5), 1e-9);
        assertEquals(0.5, MeteorRules.falloff(5, 5), 1e-9);
        assertEquals(0.0, MeteorRules.falloff(5.01, 5), 1e-9);
        assertTrue(MeteorRules.spinPerTick(0.4f) > MeteorRules.spinPerTick(3f));
    }

    @Test
    void stylesRoundTrip() {
        for (MeteorStyle s : MeteorStyle.values()) {
            assertEquals(s, MeteorStyle.of(s.ordinal()));
            assertEquals(s, MeteorStyle.byName(s.key, null));
        }
        assertEquals(6, MeteorStyle.values().length);
    }
}
