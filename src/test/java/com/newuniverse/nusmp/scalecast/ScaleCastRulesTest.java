package com.newuniverse.nusmp.scalecast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the maths of Scale casting (docs/magic/scale_casting.md) decides how hard the aura presses a foe, so it is tested without the game: the weight, the log code, the tilt of the beam, the
 * curve of the suppression and above all its HARD CAPS and FLOORS (at most 35 percent of a blow, 30 percent of spell power, half for a boss, never below one hit point, never more than the
 * blow, whatever number a config or a caller passes), the prop word round trip, the send throttle, the spring of the beam and the place the Scale floats.
 */
class ScaleCastRulesTest {
    // ---------------------------------------------------------------- weight and the log code
    @Test
    void weightIsTheExistencePointsScaledByTheLoadedShareOfTheMagiculePool() {
        assertEquals(1_000_000.0, ScaleCastRules.weight(1e6, 20, 1000, 1000), 1e-6, "a full pool brings the whole body");
        assertEquals(750_000.0, ScaleCastRules.weight(1e6, 20, 500, 1000), 1e-6, "half a pool weighs three quarters");
        assertEquals(500_000.0, ScaleCastRules.weight(1e6, 20, 0, 1000), 1e-6, "an empty pool weighs half");
        assertEquals(1_000_000.0, ScaleCastRules.weight(1e6, 20, 5, 0), 1e-6, "no pool counts as full");
        assertEquals(200.0, ScaleCastRules.weight(0, 20, 0, 0), 1e-9, "no EP: ten times the max health stands in");
        assertTrue(ScaleCastRules.weight(Double.NaN, Double.NaN, Double.NaN, Double.NaN) >= 1.0, "garbage in, a finite weight out");
        assertTrue(ScaleCastRules.weight(-5, -5, -5, -5) >= 1.0);
    }

    @Test
    void theLogCodeRoundTripsAndIsMonotonic() {
        assertEquals(120, ScaleCastRules.quantize(1e6));
        assertEquals(0, ScaleCastRules.quantize(0.2));
        assertEquals(255, ScaleCastRules.quantize(1e30));
        assertEquals(0, ScaleCastRules.quantize(Double.NaN));
        assertEquals(1e6, ScaleCastRules.dequantize(120), 1e-3);
        int prev = -1;
        for (double w = 1; w < 1e12; w *= 1.7) {
            int q = ScaleCastRules.quantize(w);
            assertTrue(q >= prev, "quantize is monotonic");
            prev = q;
            double back = ScaleCastRules.dequantize(q);
            assertTrue(back / w < 1.13 && w / back < 1.13, "the code is within 12 percent of the weight: " + w + " -> " + back);
        }
        assertEquals(1.0, ScaleCastRules.decades(100, 120), 1e-9);
        assertEquals(-0.5, ScaleCastRules.decades(110, 100), 1e-9);
    }

    // ---------------------------------------------------------------- the beam
    @Test
    void theBeamTiltsTowardTheHeavierSideSymmetricallyAndNeverPastItsStop() {
        assertEquals(0f, ScaleCastRules.tilt(0.0), 1e-6f);
        assertEquals(0f, ScaleCastRules.tilt(100, 160, false), 1e-6f, "no foe weighed: level");
        for (double d = -6; d <= 6; d += 0.25) {
            float t = ScaleCastRules.tilt(d);
            assertEquals(-t, ScaleCastRules.tilt(-d), 1e-4f, "symmetric at " + d);
            assertTrue(Math.abs(t) <= ScaleCastRules.MAX_TILT + 1e-4f);
            if (d > 0) assertTrue(t > 0f, "a heavier foe tilts toward the foe");
        }
        assertTrue(ScaleCastRules.tilt(1.0) < ScaleCastRules.tilt(2.0), "more difference, more tilt");
        assertEquals(ScaleCastRules.MAX_TILT, ScaleCastRules.tilt(1e9), 1e-3f);
        assertEquals(0f, ScaleCastRules.tilt(Double.NaN), 1e-6f);
        assertTrue(ScaleCastRules.tilt(100, 160, true) > 12f, "three decades heavier tilts a lot");
    }

    @Test
    void theSpringSettlesOnItsGoalWithABoundedOvershoot() {
        float[] st = {20f, 0f};
        float min = 0f;
        for (int i = 0; i < 80; i++) {
            ScaleCastRules.spring(st, 0f, 1f);
            min = Math.min(min, st[0]);
        }
        assertTrue(Math.abs(st[0]) < 0.3f && Math.abs(st[1]) < 0.1f, "settled: " + st[0]);
        assertTrue(min > -8f, "the overshoot stays small: " + min);
        float[] big = {0f, 0f};
        for (int i = 0; i < 300; i++) ScaleCastRules.spring(big, 24f, 2.5f);
        assertTrue(Math.abs(big[0] - 24f) < 1f, "a long frame (2.5 ticks) does not blow the spring up: " + big[0]);
        float[] wild = {0f, 0f};
        ScaleCastRules.spring(wild, 10f, 1000f);
        assertTrue(Float.isFinite(wild[0]) && Math.abs(wild[0]) < 100f, "the step is clamped");
    }

    @Test
    void theWeighingSwingRingsDownAndEndsAndAlternates() {
        assertEquals(0f, ScaleCastRules.weighSwing(-1f, 1f), 1e-9f);
        assertEquals(0f, ScaleCastRules.weighSwing(37f, 1f), 1e-9f);
        float early = 0, late = 0;
        for (float a = 0; a <= 36; a += 0.25f) {
            float v = ScaleCastRules.weighSwing(a, 1f);
            assertEquals(-v, ScaleCastRules.weighSwing(a, -1f), 1e-5f, "the sign alternates the swing");
            if (a < 8) early = Math.max(early, Math.abs(v)); else if (a > 28) late = Math.max(late, Math.abs(v));
        }
        assertTrue(early > 8f && early < 18f, "the swing starts visibly: " + early);
        assertTrue(late < early * 0.2f, "and rings down: " + late);
        assertEquals(0f, ScaleCastRules.panBob(40f), 1e-9f);
        assertTrue(Math.abs(ScaleCastRules.panBob(2f)) <= 0.06f + 1e-6f);
    }

    @Test
    void theLoadOfAPanIsItsShareOfTheHeavierSideAndNeverVanishes() {
        assertEquals(1f, ScaleCastRules.load(160, 100), 1e-6f);
        assertEquals(100f / 160f, ScaleCastRules.load(100, 160), 1e-6f);
        assertEquals(0.15f, ScaleCastRules.load(1, 200), 1e-6f, "never below the floor of the glow");
        assertEquals(1f, ScaleCastRules.load(0, 0), 1e-6f);
    }

    // ---------------------------------------------------------------- the aura: curve, caps, floors
    @Test
    void theCurveIsSmoothMonotonicAndPinnedAtItsEnds() {
        assertEquals(1.0, ScaleCastRules.strength(ScaleCastRules.FULL_AT), 1e-9);
        assertEquals(1.0, ScaleCastRules.strength(-9), 1e-9);
        assertEquals(0.0, ScaleCastRules.strength(ScaleCastRules.NONE_AT), 1e-9);
        assertEquals(0.0, ScaleCastRules.strength(9), 1e-9);
        assertEquals(0.15625, ScaleCastRules.strength(0.0), 1e-9, "at equal weight a sixth of the cap");
        double prev = 2;
        for (double d = -4; d <= 4; d += 0.05) {
            double s = ScaleCastRules.strength(d);
            assertTrue(s <= prev + 1e-12 && s >= 0 && s <= 1, "monotonic and in 0..1 at " + d);
            prev = s;
        }
        assertEquals(ScaleCastRules.strength(0.0), ScaleCastRules.strength(Double.NaN), 1e-12, "garbage is treated as no difference");
    }

    @Test
    void theReductionNeverPassesTheHardCapNoMatterTheInput() {
        for (double d = -8; d <= 8; d += 0.1) {
            for (double cfg : new double[]{0.0, 0.1, 0.35, 0.5, 5.0, 1e9, -3.0, Double.NaN, Double.POSITIVE_INFINITY}) {
                double r = ScaleCastRules.reduction(d, false, cfg), b = ScaleCastRules.reduction(d, true, cfg);
                assertTrue(r >= 0 && r <= ScaleCastRules.DAMAGE_CAP + 1e-12, "reduction " + r + " at " + d + " cap " + cfg);
                assertTrue(b >= 0 && b <= r + 1e-12 && b <= ScaleCastRules.DAMAGE_CAP * ScaleCastRules.BOSS_FACTOR + 1e-12, "a boss feels at most half: " + b);
                assertTrue(r == 0 || r >= ScaleCastRules.DEAD_ZONE, "no reduction inside the dead zone");
            }
        }
        assertEquals(0.35, ScaleCastRules.reduction(-3, false, 0.35), 1e-9);
        assertEquals(0.175, ScaleCastRules.reduction(-3, true, 0.35), 1e-9);
        assertEquals(0.0, ScaleCastRules.reduction(0.5, false, 0.35), 1e-9, "a foe 3 times heavier is not pressed");
        assertEquals(0.0, ScaleCastRules.reduction(2.0, false, 0.35), 1e-9);
        assertEquals(0.1, ScaleCastRules.reduction(-3, false, 0.1), 1e-9, "a config can only make it gentler");
        assertEquals(0.35, ScaleCastRules.clampCap(9.0), 1e-12);
        assertEquals(0.0, ScaleCastRules.clampCap(-1.0), 1e-12);
    }

    @Test
    void theSpellPowerReductionHasItsOwnLowerCap() {
        for (double dmg = -1; dmg <= 2; dmg += 0.01) {
            double m = ScaleCastRules.magicReduction(dmg);
            assertTrue(m >= 0 && m <= ScaleCastRules.MAGIC_CAP + 1e-12, "magic reduction " + m);
            assertTrue(ScaleCastRules.magicReduction(dmg, 0.1) <= 0.1 + 1e-12, "a lower server cap holds");
            assertTrue(ScaleCastRules.magicReduction(dmg, 9.0) <= ScaleCastRules.MAGIC_CAP + 1e-12, "a higher one is held to the hard cap");
        }
        assertEquals(0.2975, ScaleCastRules.magicReduction(0.35), 1e-9);
        assertEquals(0.0, ScaleCastRules.magicReduction(0.02), 1e-9, "inside the dead zone");
        assertEquals(1.0, ScaleCastRules.magicMultiplier(0.0), 1e-12);
        assertEquals(0.7, ScaleCastRules.magicMultiplier(0.3), 1e-12);
        assertEquals(0.7, ScaleCastRules.magicMultiplier(99.0), 1e-12, "the multiplier never drops under 0.70");
        assertEquals(1.0, ScaleCastRules.magicMultiplier(-5.0), 1e-12, "and never rises over 1: the aura only takes");
    }

    @Test
    void theAppliedDamageIsNeverMoreThanTheBlowNeverUnderTheFloorAndNeverMoreThanTheCapTakesOff() {
        for (float amount : new float[]{0f, 0.1f, 0.99f, 1f, 1.01f, 1.4f, 2f, 5f, 20f, 400f, 1e6f}) {
            for (double red : new double[]{0.0, 0.01, 0.2, 0.35, 0.9, 1.0, 5.0, -1.0, Double.NaN}) {
                float out = ScaleCastRules.apply(amount, red);
                assertTrue(out <= amount, "the aura never adds damage: " + amount + " -> " + out);
                assertTrue(out >= Math.min(amount, ScaleCastRules.DAMAGE_FLOOR), "never under the floor: " + amount + " -> " + out);
                assertTrue(out >= amount * (1.0 - ScaleCastRules.DAMAGE_CAP) - 1e-3 * amount, "never more than the cap off: " + amount + " red " + red + " -> " + out);
            }
        }
        assertEquals(6.5f, ScaleCastRules.apply(10f, 0.35), 1e-5f);
        assertEquals(6.5f, ScaleCastRules.apply(10f, 5.0), 1e-5f, "a reduction over the cap is held to it");
        assertEquals(10f, ScaleCastRules.apply(10f, 0.0), 1e-6f);
        assertEquals(1f, ScaleCastRules.apply(1.2f, 0.35), 1e-6f, "the floor: a 1.2 hit is not cut to 0.78");
        assertEquals(0.5f, ScaleCastRules.apply(0.5f, 0.35), 1e-6f, "a hit under the floor is left alone");
        assertEquals(5f, ScaleCastRules.apply(5f, Double.NaN), 1e-6f);
        assertTrue(Float.isNaN(ScaleCastRules.apply(Float.NaN, 0.3)) && ScaleCastRules.apply(Float.POSITIVE_INFINITY, 0.3) == Float.POSITIVE_INFINITY, "non finite blows are not touched");
    }

    @Test
    void severalCastersOnOneFoeNeverAddUp() {
        assertEquals(0.3, ScaleCastRules.stronger(0.3, 0.2), 1e-12);
        assertEquals(0.3, ScaleCastRules.stronger(0.2, 0.3), 1e-12);
        double combined = ScaleCastRules.stronger(ScaleCastRules.stronger(0.35, 0.35), 0.35);
        assertEquals(0.35, combined, 1e-12);
    }

    @Test
    void theCapsOfTheCountsAreTheDocumentedOnes() {
        assertEquals(8, ScaleCastRules.MAX_FOES);
        assertEquals(10, ScaleCastRules.SCAN_EVERY);
        assertEquals(32, ScaleCastRules.GLOBAL_SCALES);
        assertEquals(0.35, ScaleCastRules.DAMAGE_CAP, 1e-12);
        assertEquals(0.30, ScaleCastRules.MAGIC_CAP, 1e-12);
        assertTrue(ScaleCastRules.ENTRY_LIFE > ScaleCastRules.SCAN_EVERY * 2, "an entry outlives the gap between two scans, so a foe is not freed between them");
        assertEquals(4.0, ScaleCastRules.clampRadius(1.0), 1e-12);
        assertEquals(16.0, ScaleCastRules.clampRadius(500.0), 1e-12);
        assertEquals(12.0, ScaleCastRules.clampRadius(12.0), 1e-12);
        assertEquals(4.0, ScaleCastRules.clampRadius(Double.NaN), 1e-12, "garbage falls to the smallest radius, not to a huge one");
    }

    // ---------------------------------------------------------------- the prop word
    @Test
    void thePropWordRoundTripsEveryField() {
        for (int cq : new int[]{0, 1, 100, 255})
            for (int fq : new int[]{0, 77, 255})
                for (int pct : new int[]{0, 5, 35, 63})
                    for (int pulse = 0; pulse < 8; pulse++)
                        for (int flags = 0; flags < 8; flags++) {
                            boolean foe = (flags & 1) != 0, aura = (flags & 2) != 0, fade = (flags & 4) != 0;
                            int w = ScaleCastRules.pack(cq, fq, foe, pct, 12.0, aura, pulse, fade);
                            assertEquals(cq, ScaleCastRules.casterQ(w));
                            assertEquals(fq, ScaleCastRules.foeQ(w));
                            assertEquals(pct, ScaleCastRules.percent(w));
                            assertEquals(12, ScaleCastRules.radius(w));
                            assertEquals(pulse, ScaleCastRules.pulse(w));
                            assertEquals(foe, ScaleCastRules.hasFoe(w));
                            assertEquals(aura, ScaleCastRules.aura(w));
                            assertEquals(fade, ScaleCastRules.fading(w));
                        }
        assertEquals(16, ScaleCastRules.radius(ScaleCastRules.pack(0, 0, false, 0, 500.0, false, 0, false)), "the radius is clamped into 4..16");
        assertEquals(4, ScaleCastRules.radius(ScaleCastRules.pack(0, 0, false, 0, 0.5, false, 0, false)));
        assertEquals(255, ScaleCastRules.casterQ(ScaleCastRules.pack(9999, 0, false, 0, 12, false, 0, false)), "out of range codes are clamped, not wrapped");
        assertEquals(0, ScaleCastRules.casterQ(ScaleCastRules.pack(-9, 0, false, 0, 12, false, 0, false)));
        assertEquals(3, ScaleCastRules.pulse(ScaleCastRules.pack(0, 0, false, 0, 12, false, 11, false)), "the pulse wraps at 8");
    }

    @Test
    void theWordIsSentAtOnceForEventsAndThrottledForMeasurements() {
        int base = ScaleCastRules.pack(100, 110, true, 10, 12, true, 2, false);
        assertFalse(ScaleCastRules.shouldSetWord(base, base, 100, 0), "nothing changed");
        int measured = ScaleCastRules.pack(101, 112, true, 11, 12, true, 2, false);
        assertFalse(ScaleCastRules.shouldSetWord(base, measured, 105, 100), "new numbers within ten ticks wait");
        assertTrue(ScaleCastRules.shouldSetWord(base, measured, 110, 100), "and go out after ten");
        assertTrue(ScaleCastRules.shouldSetWord(base, ScaleCastRules.pack(100, 110, true, 10, 12, true, 3, false), 101, 100), "a cast pulse goes at once");
        assertTrue(ScaleCastRules.shouldSetWord(base, ScaleCastRules.pack(100, 110, true, 10, 12, false, 2, false), 101, 100), "the aura switching goes at once");
        assertTrue(ScaleCastRules.shouldSetWord(base, ScaleCastRules.pack(100, 110, false, 10, 12, true, 2, false), 101, 100), "a foe appearing or going goes at once");
        assertTrue(ScaleCastRules.shouldSetWord(base, ScaleCastRules.pack(100, 110, true, 10, 12, true, 2, true), 101, 100), "the fade goes at once");
    }

    // ---------------------------------------------------------------- opacity, place, text
    @Test
    void theScaleFadesInAndOutByItsOwnClock() {
        assertEquals(0f, ScaleCastRules.opacity(0f, -1f), 1e-6f);
        assertEquals(1f, ScaleCastRules.opacity(100f, -1f), 1e-6f);
        assertEquals(0.5f, ScaleCastRules.opacity(100f, ScaleCastRules.FADE / 2f), 1e-5f);
        assertEquals(0f, ScaleCastRules.opacity(100f, ScaleCastRules.FADE), 1e-6f);
        assertEquals(0f, ScaleCastRules.opacity(100f, 1000f), 1e-6f);
        assertEquals(1f, ScaleCastRules.popIn(50f, 10f), 1e-5f);
        assertEquals(0f, ScaleCastRules.popIn(0f, 10f), 1e-5f);
    }

    @Test
    void theScaleFloatsAtTheLeftShoulderAndThePlainFunctionsAgreeWithTheArray() {
        for (float yaw = -360; yaw <= 360; yaw += 37.5f) {
            double[] p = ScaleCastRules.position(10, 64, -3, yaw);
            assertEquals(p[0], ScaleCastRules.positionX(10, yaw), 1e-9);
            assertEquals(p[1], ScaleCastRules.positionY(64), 1e-9);
            assertEquals(p[2], ScaleCastRules.positionZ(-3, yaw), 1e-9);
            double dx = p[0] - 10, dz = p[2] + 3;
            assertEquals(Math.hypot(ScaleCastRules.SIDE, ScaleCastRules.FRONT), Math.hypot(dx, dz), 1e-9, "the same reach at every yaw");
        }
        double[] south = ScaleCastRules.position(0, 0, 0, 0f);                      // facing +z (south): the left hand is +x (east)
        assertTrue(south[0] > 1.0 && Math.abs(south[2] - ScaleCastRules.FRONT) < 1e-9, "left of a south facing caster is east, a little ahead");
        double[] east = ScaleCastRules.position(0, 0, 0, -90f);                     // facing +x: the left hand is -z
        assertTrue(east[2] < -1.0, "left of an east facing caster is north");
        assertEquals(65f, ScaleCastRules.yaw(0f), 1e-6f);
        assertEquals(370f, ScaleCastRules.approachDegrees(350f, 10f, 1f), 1e-3f, "the short way round (350 to 10 is 20 degrees on, i.e. 370)");
        assertEquals(190f, ScaleCastRules.approachDegrees(170f, -170f, 1f), 1e-3f);
        assertEquals(355f, ScaleCastRules.approachDegrees(350f, 10f, 0.25f), 1e-3f);
    }

    @Test
    void theReadoutFormatsWeightsShortly() {
        assertEquals("0", ScaleCastRules.format(0));
        assertEquals("940", ScaleCastRules.format(940));
        assertEquals("1.0K", ScaleCastRules.format(1000));
        assertEquals("12K", ScaleCastRules.format(12_400));
        assertEquals("3.1M", ScaleCastRules.format(3_100_000));
        assertEquals("2.5B", ScaleCastRules.format(2.5e9));
        assertEquals("7.0T", ScaleCastRules.format(7e12));
        assertEquals("1.0M", ScaleCastRules.format(999_700), "999.7K does not print as 1000K");
        assertTrue(ScaleCastRules.format(1.2e15).contains("e"));
        assertEquals("0", ScaleCastRules.format(Double.NaN));
        assertEquals("0", ScaleCastRules.format(-4));
    }
}
