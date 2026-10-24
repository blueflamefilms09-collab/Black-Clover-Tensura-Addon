package com.newuniverse.nusmp.ultimate;

import com.newuniverse.nusmp.ultimate.UltimateRules.Cancel;
import com.newuniverse.nusmp.ultimate.UltimateRules.Start;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: every number that bounds an Ultimate (cost, caps, cancel rules, damage clamps, cooldown, packets) is pure code in {@link UltimateRules}, pinned here without a game: a change that makes an Ultimate
 * cheaper, longer-lived, wider or more expensive for the network has to change a pin in the same commit and say why. Pure.
 */
class UltimateRulesTest {
    private static Start start(boolean enabled, boolean gate, long now, long cd, long lock, boolean busy, int active, int cap, double cur, double max, boolean creative) {
        return UltimateRules.start(enabled, gate, now, cd, lock, busy, active, cap, cur, max, creative);
    }

    // ------------------------------------------------------------------ the price
    @Test
    void theCasterMustBeAtMaxMagiculeToStart() {
        assertTrue(UltimateRules.atMax(1000, 1000));
        assertTrue(UltimateRules.atMax(991, 1000), "99 percent counts as full (regeneration, rounding)");
        assertFalse(UltimateRules.atMax(989, 1000));
        assertFalse(UltimateRules.atMax(0, 0), "a zero maximum never counts as full");
        assertFalse(UltimateRules.atMax(500, 1000));
    }

    @Test
    void theReserveMustBeHeldDuringTheWindUp() {
        assertTrue(UltimateRules.reserveHeld(1000, 1000));
        assertTrue(UltimateRules.reserveHeld(900, 1000));
        assertFalse(UltimateRules.reserveHeld(899, 1000), "a spell cast in the middle of the wind-up breaks the reserve");
        assertFalse(UltimateRules.reserveHeld(5, 0));
    }

    @Test
    void theReleaseTakesAllTheCurrentMagiculeAndNeverMore() {
        assertEquals(1234.5, UltimateRules.release(1234.5), 1e-9);
        assertEquals(0.0, UltimateRules.release(-5), 1e-9);
        assertEquals(0.0, UltimateRules.release(Double.NaN), 1e-9);
        assertEquals(0.0, UltimateRules.release(0), 1e-9);
    }

    // ------------------------------------------------------------------ starting
    @Test
    void startRefusalsComeInAFixedOrder() {
        assertEquals(Start.OK, start(true, true, 100, 0, 0, false, 0, 2, 1000, 1000, false));
        assertEquals(Start.DISABLED, start(false, false, 100, 999, 999, true, 9, 2, 0, 1000, false), "switched off beats everything");
        assertEquals(Start.GATE, start(true, false, 100, 999, 999, true, 9, 2, 0, 1000, false));
        assertEquals(Start.BUSY, start(true, true, 100, 999, 999, true, 9, 2, 0, 1000, false));
        assertEquals(Start.COOLDOWN, start(true, true, 100, 999, 999, false, 9, 2, 0, 1000, false));
        assertEquals(Start.LOCKOUT, start(true, true, 100, 0, 999, false, 9, 2, 0, 1000, false));
        assertEquals(Start.GLOBAL_CAP, start(true, true, 100, 0, 0, false, 2, 2, 0, 1000, false));
        assertEquals(Start.NOT_FULL, start(true, true, 100, 0, 0, false, 0, 2, 600, 1000, false));
    }

    @Test
    void theCooldownEndsExactlyAtItsTick() {
        assertEquals(Start.COOLDOWN, start(true, true, 99, 100, 0, false, 0, 2, 1000, 1000, false));
        assertEquals(Start.OK, start(true, true, 100, 100, 0, false, 0, 2, 1000, 1000, false));
    }

    @Test
    void creativeSkipsOnlyTheManaRule() {
        assertEquals(Start.OK, start(true, true, 100, 0, 0, false, 0, 2, 0, 1000, true));
        assertEquals(Start.COOLDOWN, start(true, true, 100, 500, 0, false, 0, 2, 0, 1000, true));
        assertEquals(Start.GLOBAL_CAP, start(true, true, 100, 0, 0, false, 2, 2, 0, 1000, true));
        assertEquals(Start.GATE, start(true, false, 100, 0, 0, false, 0, 2, 0, 1000, true));
    }

    @Test
    void theGlobalCapIsClampedToOneToEight() {
        assertEquals(1, UltimateRules.simultaneousCap(0));
        assertEquals(1, UltimateRules.simultaneousCap(-4));
        assertEquals(2, UltimateRules.simultaneousCap(2));
        assertEquals(8, UltimateRules.simultaneousCap(500));
        assertEquals(Start.GLOBAL_CAP, start(true, true, 0, 0, 0, false, 8, 500, 1000, 1000, false), "a config of 500 still means 8 at most");
        assertEquals(Start.GLOBAL_CAP, start(true, true, 0, 0, 0, false, 1, 0, 1000, 1000, false), "a config of 0 still means 1");
    }

    @Test
    void theCooldownCanNeverBeConfiguredUnderFiveMinutes() {
        assertEquals(6000, UltimateRules.cooldownTicks(0));
        assertEquals(6000, UltimateRules.cooldownTicks(1));
        assertEquals(18000, UltimateRules.cooldownTicks(18000));
        assertEquals(144000, UltimateRules.cooldownTicks(Integer.MAX_VALUE));
        assertTrue(UltimateRules.DEFAULT_COOLDOWN_TICKS >= 3 * UltimateRules.MIN_COOLDOWN_TICKS, "the shipped cooldown is long");
    }

    // ------------------------------------------------------------------ cancelling
    @Test
    void damageBreaksTheCastAtTheConfiguredFractionOfMaxHealth() {
        assertFalse(UltimateRules.interrupts(6.9, 20, 0.35));
        assertTrue(UltimateRules.interrupts(7.0, 20, 0.35));
        assertTrue(UltimateRules.interrupts(100, 20, 0.35));
        assertFalse(UltimateRules.interrupts(1.9, 20, 0.0), "a config of 0 is clamped to 10 percent, not to every blow");
        assertTrue(UltimateRules.interrupts(2.0, 20, 0.0));
        assertFalse(UltimateRules.interrupts(19, 20, 5.0), "a config over 100 percent is clamped to 100");
        assertFalse(UltimateRules.interrupts(1, 0, 0.35), "no health data never interrupts");
    }

    @Test
    void sneakingLetsGoOnlyAfterTheFirstSecondAndAfterAHold() {
        assertFalse(UltimateRules.sneakCancels(true, 5, 100), "the key that started the cast may still be down");
        assertFalse(UltimateRules.sneakCancels(true, 100, 14));
        assertTrue(UltimateRules.sneakCancels(true, 100, 15));
        assertFalse(UltimateRules.sneakCancels(false, 100, 100), "switched off in the config");
    }

    @Test
    void everyCancelHasAMessageAndOnlyServerEndsAreFreeOfTheLockout() {
        for (Cancel c : Cancel.values()) {
            assertFalse(UltimateRules.reason(c).isBlank(), c.name());
            long lock = UltimateRules.lockoutUntil(1000, c);
            if (c == Cancel.SHUTDOWN || c == Cancel.ADMIN) assertEquals(0L, lock, c.name());
            else assertEquals(1000 + UltimateRules.FAIL_LOCKOUT_TICKS, lock, c.name());
        }
        for (Start s : Start.values()) if (s != Start.OK) assertFalse(UltimateRules.reason(s).isBlank(), s.name());
        assertEquals("", UltimateRules.reason(Start.OK));
    }

    @Test
    void aBrokenCastCostsNothingInMagiculeByConstruction() {
        // the magicule only changes at the release: no cancel path has an amount, and the lockout is the whole price
        assertTrue(UltimateRules.FAIL_LOCKOUT_TICKS < UltimateRules.MIN_COOLDOWN_TICKS);
        assertTrue(UltimateRules.FAIL_LOCKOUT_TICKS >= 100, "a lockout that is long enough to stop start-cancel-start loops");
    }

    // ------------------------------------------------------------------ damage
    @Test
    void theFalloffGoesFromOneToFiftyFivePercent() {
        assertEquals(1.0, UltimateRules.falloff(0, 16), 1e-9);
        assertEquals(0.55, UltimateRules.falloff(16, 16), 1e-9);
        assertEquals(0.55, UltimateRules.falloff(99, 16), 1e-9);
        assertEquals(1.0, UltimateRules.falloff(5, 0), 1e-9);
        double prev = 2;
        for (double d = 0; d <= 16; d += 1) {
            double f = UltimateRules.falloff(d, 16);
            assertTrue(f <= prev + 1e-12, "never grows with distance");
            prev = f;
        }
    }

    @Test
    void thePulseSharesAddUpToOneAndTheFirstPulseIsTheBigOne() {
        double sum = 0;
        for (double s : UltimateRules.PULSE_SHARE) sum += s;
        assertEquals(1.0, sum, 1e-9);
        assertEquals(UltimateRules.PULSES, UltimateRules.PULSE_SHARE.length);
        assertTrue(UltimateRules.PULSE_SHARE[0] > UltimateRules.PULSE_SHARE[1] && UltimateRules.PULSE_SHARE[1] > UltimateRules.PULSE_SHARE[2]);
    }

    @Test
    void aPulseNeverExceedsTheRawClampWhateverTheConfigSays() {
        for (int pulse = 0; pulse < UltimateRules.PULSES; pulse++)
            for (float base : new float[]{-5f, 0f, 10f, 40f, 80f, 9999f, Float.MAX_VALUE})
                for (double scale : new double[]{-3, 0, 1, 2, 50, Double.NaN, Double.POSITIVE_INFINITY}) {
                    float raw = UltimateRules.pulseRaw(base, scale, pulse, 0.0, 16);
                    assertTrue(raw >= 0f && raw <= UltimateRules.MAX_RAW, "pulse " + pulse + " base " + base + " scale " + scale + " gave " + raw);
                }
        assertEquals(0f, UltimateRules.pulseRaw(40f, 1.0, -1, 0, 16));
        assertEquals(0f, UltimateRules.pulseRaw(40f, 1.0, UltimateRules.PULSES, 0, 16));
        assertEquals(0f, UltimateRules.pulseRaw(40f, 0.0, 0, 0, 16), "a damage scale of 0 means only the debuffs");
    }

    @Test
    void theWholeCastDealsAtMostItsBaseTimesTwiceTheScalePerTarget() {
        float total = 0;
        for (int i = 0; i < UltimateRules.PULSES; i++) total += UltimateRules.pulseRaw(40f, 1.0, i, 0.0, 16);
        assertEquals(40f, total, 1e-3f, "at the centre the three pulses add up to the base damage");
        float edge = 0;
        for (int i = 0; i < UltimateRules.PULSES; i++) edge += UltimateRules.pulseRaw(40f, 1.0, i, 16.0, 16);
        assertEquals(22f, edge, 1e-3f, "at the edge 55 percent of it");
    }

    @Test
    void debuffsAreBoundedAndHalvedOnABoss() {
        assertEquals(300, UltimateRules.debuffTicks(300, false));
        assertEquals(150, UltimateRules.debuffTicks(300, true));
        assertEquals(600, UltimateRules.debuffTicks(100000, false));
        assertEquals(0, UltimateRules.debuffTicks(-5, false));
        assertEquals(3, UltimateRules.debuffAmplifier(99));
        assertEquals(0, UltimateRules.debuffAmplifier(-2));
    }

    // ------------------------------------------------------------------ wind-up, radius, packets
    @Test
    void windUpAndRadiusStayInsideTheirBoundsWhateverTheScale() {
        for (double scale : new double[]{-1, 0, 0.1, 1, 2, 100, Double.NaN}) {
            int w = UltimateRules.windupTicks(160, scale);
            double r = UltimateRules.radius(16, scale);
            assertTrue(w >= UltimateRules.MIN_WINDUP_TICKS && w <= UltimateRules.MAX_WINDUP_TICKS, "windup " + w + " at " + scale);
            assertTrue(r >= UltimateRules.MIN_RADIUS && r <= UltimateRules.MAX_RADIUS, "radius " + r + " at " + scale);
        }
        assertTrue(UltimateRules.MIN_WINDUP_TICKS >= 100, "a wind-up is at least five seconds: it has to be long");
    }

    @Test
    void theWorstCaseNumbersOfEveryCastAreBounded() {
        for (int windup = UltimateRules.MIN_WINDUP_TICKS; windup <= UltimateRules.MAX_WINDUP_TICKS; windup++) {
            assertTrue(UltimateRules.packetsFor(windup) <= UltimateRules.MAX_EFFECT_PACKETS, "effect packets at windup " + windup + ": " + UltimateRules.packetsFor(windup));
            assertTrue(UltimateRules.particlePacketsFor(windup) <= 2 * (UltimateRules.MAX_WINDUP_TICKS / UltimateRules.FX_EMIT_PERIOD + 1));
        }
        assertEquals(72, UltimateRules.MAX_TARGETS * UltimateRules.PULSES, "at most 72 hits in one cast");
        assertTrue(UltimateRules.MAX_CANDIDATES <= 128 && UltimateRules.MAX_CANDIDATES >= UltimateRules.MAX_TARGETS);
        assertTrue(UltimateRules.MAX_TARGETS <= 24 && UltimateRules.PULSES == 3);
    }

    @Test
    void theChargePictureIsResentAtMostSixTimes() {
        int n = 0;
        for (int age = 0; age < UltimateRules.MAX_WINDUP_TICKS; age++) if (UltimateRules.chargeFxDue(age, UltimateRules.MAX_WINDUP_TICKS)) n++;
        assertEquals(6, n);
        assertFalse(UltimateRules.chargeFxDue(40, 40), "nothing at or after the release");
    }

    @Test
    void pulseTicksAreTenTicksApart() {
        assertEquals(0, UltimateRules.pulseTick(0));
        assertEquals(10, UltimateRules.pulseTick(1));
        assertEquals(20, UltimateRules.pulseTick(2));
        assertEquals(20, UltimateRules.pulseTick(99));
        assertEquals(0, UltimateRules.pulseTick(-3));
    }

    @Test
    void theClampsAreNanSafe() {
        assertEquals(0.5, UltimateRules.clampDouble(Double.NaN, 0.5, 2.0), 1e-9);
        assertEquals(2.0, UltimateRules.clampDouble(Double.POSITIVE_INFINITY, 0.5, 2.0), 1e-9);
        assertEquals(5, UltimateRules.clampInt(5, 1, 9));
        assertEquals(1, UltimateRules.clampInt(-5, 1, 9));
        assertEquals(9, UltimateRules.clampInt(500, 1, 9));
    }
}
