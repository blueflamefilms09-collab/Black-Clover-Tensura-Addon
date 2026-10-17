package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** What Escanor's Sunshine means for the fight: stronger by day, never helpless at night, the Gates hold a floor, the power cap unlocks the moves. */
class SunshineTest {
    @Test
    void daylightMakesHimHitHarderAndTakeLess() {
        assertEquals(0.744, Sunshine.damageScale(SunClock.NIGHT_FLOOR), 1e-9);
        assertEquals(1.40, Sunshine.damageScale(1.0), 1e-9);
        assertEquals(1.0316, Sunshine.taken(SunClock.NIGHT_FLOOR), 1e-4);
        assertEquals(0.72, Sunshine.taken(1.0), 1e-9);
        double d = 0, t = 9;
        for (double s = 0; s <= 1.0; s += 0.05) {
            assertTrue(Sunshine.damageScale(s) >= d, "damage never falls with the sun");
            assertTrue(Sunshine.taken(s) <= t, "he takes less as the sun climbs");
            d = Sunshine.damageScale(s);
            t = Sunshine.taken(s);
        }
        assertTrue(Sunshine.taken(1.0) > 0.5, "the sun shields him, it never makes him immune");
        assertTrue(Sunshine.damageScale(SunClock.NIGHT_FLOOR) > 0.5, "and at night he still has an axe");
        assertEquals(0.10, Sunshine.speedBonus(1.0), 1e-9);
        assertEquals(0.0, Sunshine.speedBonus(0.0), 1e-9);
        assertEquals(1.40, Sunshine.damageScale(7.0), 1e-9, "out-of-range suns are clamped");
        assertEquals(0.60, Sunshine.damageScale(-3.0), 1e-9);
    }

    @Test
    void theSunDecidesHowMuchPowerHeMayDraw() {
        assertEquals(20, Sunshine.powerCap(SunClock.NIGHT_FLOOR));
        assertEquals(20, Sunshine.powerCap(0.29));
        assertEquals(40, Sunshine.powerCap(0.30));
        assertEquals(40, Sunshine.powerCap(0.49));
        assertEquals(60, Sunshine.powerCap(0.50));
        assertEquals(80, Sunshine.powerCap(0.72));
        assertEquals(80, Sunshine.powerCap(0.89));
        assertEquals(100, Sunshine.powerCap(0.90));
        assertEquals(100, Sunshine.powerCap(1.0));
        assertEquals(20, Sunshine.effectivePower(100, 0.18), "a boss at the Gates in the dark draws no more than the dark allows");
        assertEquals(60, Sunshine.effectivePower(60, 1.0), "and a high sun cannot give more than his health has reached");
        assertEquals(80, Sunshine.effectivePower(100, 0.8));
        assertEquals(100, Sunshine.effectivePower(100, 0.95));
    }

    @Test
    void theGatesHoldAFloorSoTheLastQuarterIsNeverAWalkover() {
        assertEquals(0.18, Sunshine.effective(0.18, false, Sunshine.GATES_FLOOR), 1e-9);
        assertEquals(0.60, Sunshine.effective(0.18, true, Sunshine.GATES_FLOOR), 1e-9);
        assertEquals(0.95, Sunshine.effective(0.95, true, Sunshine.GATES_FLOOR), 1e-9, "the floor never lowers a high sun");
        assertEquals(1.0, Sunshine.effective(5.0, false, 0.6), 1e-9);
        assertEquals(0.0, Sunshine.effective(-1.0, false, 0.6), 1e-9);
        assertEquals(60, Sunshine.powerCap(Sunshine.GATES_FLOOR), "even the floor allows the 60% moves, but not Cruel Sun or The One");
        assertTrue(Sunshine.powerCap(Sunshine.GATES_FLOOR) < 80);
    }

    @Test
    void theHeatBurnsOnlyWhenTheSunIsHighAndReachesFurtherAtNoon() {
        assertEquals(0.0, Sunshine.auraRadius(0.44), 0.0);
        assertEquals(0.0, Sunshine.auraRadius(SunClock.NIGHT_FLOOR), 0.0);
        assertTrue(Sunshine.auraRadius(Sunshine.AURA_FROM) > 4.5);
        assertEquals(6.5, Sunshine.auraRadius(1.0), 1e-9);
        assertTrue(Sunshine.auraRadius(1.0) < 8, "a ranged player is always out of it");
        assertTrue(Sunshine.auraDamage(1.0) > Sunshine.auraDamage(0.5));
        assertTrue(Sunshine.auraDamage(1.0) < 5, "a pulse is a nag, not a nuke");
    }

    @Test
    void theGlowRunsFromNothingToFull() {
        assertEquals(0.0, Sunshine.glow(SunClock.NIGHT_FLOOR), 1e-9);
        assertEquals(1.0, Sunshine.glow(1.0), 1e-9);
        assertEquals(0.0, Sunshine.glow(0.0), 1e-9, "clamped");
        assertTrue(Sunshine.glow(0.5) > 0.3 && Sunshine.glow(0.5) < 0.5);
    }

    @Test
    void theFormHasHysteresisSoItNeverFlickers() {
        assertEquals(0, Sunshine.form(0, 0.18));
        assertEquals(0, Sunshine.form(0, 0.46), "up needs 0.47");
        assertEquals(1, Sunshine.form(0, 0.47));
        assertEquals(1, Sunshine.form(1, 0.44), "down needs below 0.43");
        assertEquals(0, Sunshine.form(1, 0.42));
        assertEquals(1, Sunshine.form(1, 0.91), "The One needs 0.92");
        assertEquals(2, Sunshine.form(1, 0.92));
        assertEquals(2, Sunshine.form(2, 0.89), "back down needs below 0.88");
        assertEquals(1, Sunshine.form(2, 0.87));
        assertEquals(2, Sunshine.form(0, 1.0), "a jump from the frail man to noon passes through the middle form");
        assertEquals(0, Sunshine.form(2, 0.1), "and the other way");
        assertEquals(0, Sunshine.form(-5, 0.1));
        assertEquals(2, Sunshine.form(9, 1.0));
    }

    @Test
    void theWholeDayProducesAPlayableFight() {
        int nights = 0, ones = 0;
        for (int t = 0; t < 24_000; t += 10) {
            double s = SunClock.sunshine(t);
            assertTrue(Sunshine.effectivePower(100, s) >= 20, "he always has the 20% moves");
            assertTrue(Sunshine.effective(s, true, Sunshine.GATES_FLOOR) >= Sunshine.GATES_FLOOR);
            if (Sunshine.powerCap(s) == 20) nights++;
            if (Sunshine.powerCap(s) == 100) ones++;
        }
        assertTrue(nights > 0 && ones > 0, "both a night and a noon exist");
        assertTrue(ones < nights, "the party sees far more of the weak sun than of The One");
    }
}
