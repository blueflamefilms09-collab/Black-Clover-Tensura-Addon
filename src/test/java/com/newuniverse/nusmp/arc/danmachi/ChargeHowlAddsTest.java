package com.newuniverse.nusmp.arc.danmachi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The numbers behind the charges (Minotaur, Goliath), the Howl and the monsters a roar calls. */
class ChargeHowlAddsTest {
    // ------------------------------------------------------------------ charge
    @Test
    void aRunTakesAsLongAsItsLengthOverItsSpeed() {
        assertEquals(16, ChargeMath.ticks(24, 1.5));
        assertEquals(15, ChargeMath.ticks(18, 1.2));
        assertEquals(17, ChargeMath.ticks(24, 1.45), "rounded up: the run is never cut short");
        assertEquals(ChargeMath.MIN_TICKS, ChargeMath.ticks(2, 1.5));
        assertEquals(ChargeMath.MIN_TICKS, ChargeMath.ticks(0, 1.5));
        assertEquals(ChargeMath.MIN_TICKS, ChargeMath.ticks(24, 0), "a stopped bull does not divide by zero");
        assertEquals(ChargeMath.MIN_TICKS, ChargeMath.ticks(-5, 1.5));
    }

    @Test
    void aBullThatRamsAWallLearnsAndTheStunShortensWithPower() {
        assertEquals(60, ChargeMath.crashStun(20));
        assertEquals(54, ChargeMath.crashStun(40));
        assertEquals(48, ChargeMath.crashStun(60));
        assertEquals(42, ChargeMath.crashStun(80));
        assertEquals(36, ChargeMath.crashStun(100));
        assertTrue(ChargeMath.crashStun(100) >= 30, "never a blink");
        for (int p = 20; p < 100; p += 10) assertTrue(ChargeMath.crashStun(p) >= ChargeMath.crashStun(p + 10), "never longer later at " + p);
        assertTrue(ChargeMath.CRASH_BONUS > 1.0f);
    }

    @Test
    void aRampageChainsMoreDashesLaterAndBreathesBetween() {
        assertEquals(0, ChargeMath.rampageDashes(40), "not before 60%");
        assertEquals(3, ChargeMath.rampageDashes(60));
        assertEquals(3, ChargeMath.rampageDashes(70));
        assertEquals(4, ChargeMath.rampageDashes(80));
        assertEquals(5, ChargeMath.rampageDashes(100));
        assertEquals(14, ChargeMath.rampageBreath(60));
        assertEquals(8, ChargeMath.rampageBreath(100));
        for (int p = 20; p <= 100; p += 20) assertTrue(ChargeMath.rampageBreath(p) >= 8, "always a beat between dashes");
        assertEquals(8.8, ChargeMath.dashLength(11, 5), 1e-9, "five dashes of 11 would leave the arena: 44 / 5");
        assertEquals(11, ChargeMath.dashLength(11, 3), 1e-9);
        assertEquals(4.0, ChargeMath.dashLength(2, 3), 1e-9, "never a hop shorter than a bull's length");
        assertEquals(44.0, ChargeMath.dashLength(60, 0), 1e-9);
        assertTrue(ChargeMath.DASH_TELL >= 12, "every dash is shown at least twelve ticks ahead");
        double[] p = ChargeMath.pointOn(1, 2, 0, 1, 5);
        assertEquals(1.0, p[0], 1e-12);
        assertEquals(7.0, p[1], 1e-12);
    }

    // ------------------------------------------------------------------ howl
    @Test
    void theHowlFrightensTheWeakInsideTheRingAndNobodyElse() {
        assertTrue(HowlMath.afraid(3, 2, false, true, 8, 12), "a Level 2 fighter in the ring of a Level 3 monster");
        assertFalse(HowlMath.afraid(3, 3, false, true, 8, 12), "level with the monster");
        assertFalse(HowlMath.afraid(3, 5, false, true, 8, 12), "above it");
        assertFalse(HowlMath.afraid(3, 2, false, true, 13, 12), "outside the ring");
        assertTrue(HowlMath.afraid(3, 2, false, true, 12, 12), "on the ring itself");
        assertFalse(HowlMath.afraid(3, 1, true, true, 8, 12), "anti-magic in hand negates it, as Welf's sword did");
        assertFalse(HowlMath.afraid(3, 1, false, false, 8, 12), "a creative player or a spectator is not a fighter");
    }

    @Test
    void theFearGrowsWithTheGapAndWithThePower() {
        assertEquals(1, HowlMath.slowAmplifier(3, 2));
        assertEquals(2, HowlMath.slowAmplifier(4, 2));
        assertEquals(2, HowlMath.slowAmplifier(5, 2));
        assertEquals(3, HowlMath.slowAmplifier(6, 2));
        assertEquals(3, HowlMath.slowAmplifier(9, 1), "never beyond Slowness IV");
        assertEquals(1, HowlMath.slowAmplifier(2, 5), "a gap below 1 counts as 1 (it is only asked for the afraid)");
        assertEquals(50, HowlMath.fearTicks(20));
        assertEquals(74, HowlMath.fearTicks(100));
        assertEquals(12.0, HowlMath.radius(12, 20), 1e-9);
        assertEquals(14.0, HowlMath.radius(12, 100), 1e-9);
    }

    // ------------------------------------------------------------------ adds
    @Test
    void theCapGrowsWithPowerAndObeysTheConfigAndTheHardCeiling() {
        assertEquals(2, AddPlan.cap(20, 6));
        assertEquals(3, AddPlan.cap(40, 6));
        assertEquals(4, AddPlan.cap(60, 6));
        assertEquals(5, AddPlan.cap(80, 6));
        assertEquals(6, AddPlan.cap(100, 6));
        assertEquals(3, AddPlan.cap(100, 3), "the config can lower it");
        assertEquals(0, AddPlan.cap(100, 0), "and switch it off");
        assertEquals(0, AddPlan.cap(100, -4));
        assertEquals(6, AddPlan.cap(100, 500), "a huge config value is held by the power's own cap");
        assertTrue(AddPlan.HARD_CAP <= 24);
    }

    @Test
    void aRoarBringsAFewAtATimeAndNeverMoreThanTheRoom() {
        assertEquals(2, AddPlan.toSummon(2, 0, 1), "one more than the volley size");
        assertEquals(3, AddPlan.toSummon(6, 0, 2));
        assertEquals(5, AddPlan.toSummon(6, 0, 4));
        assertEquals(1, AddPlan.toSummon(6, 5, 4), "only the room left");
        assertEquals(0, AddPlan.toSummon(6, 6, 4));
        assertEquals(0, AddPlan.toSummon(6, 9, 4), "over the cap already");
        assertEquals(2, AddPlan.toSummon(6, -3, 1), "a negative count of the living is zero");
        assertEquals(2, AddPlan.toSummon(6, 0, 0), "a volley size below 1 counts as 1");
    }

    @Test
    void calledMonstersGrowStrongerAndLiveLonger() {
        assertEquals(1200, AddPlan.lifetime(20));
        assertEquals(2000, AddPlan.lifetime(100));
        assertEquals(60.0, AddPlan.health(100, 20), 1e-9);
        assertEquals(120.0, AddPlan.health(100, 100), 1e-9);
        assertEquals(7.0, AddPlan.attack(10, 20), 1e-9);
        assertEquals(11.0, AddPlan.attack(10, 100), 1e-9);
        for (int p = 20; p < 100; p += 20) {
            assertTrue(AddPlan.health(100, p + 20) > AddPlan.health(100, p));
            assertTrue(AddPlan.attack(10, p + 20) > AddPlan.attack(10, p));
            assertTrue(AddPlan.lifetime(p + 20) > AddPlan.lifetime(p));
        }
    }
}
