package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Escanor's sun: highest at noon, lowest at midnight, a phase for every hour, and a fight clock that runs on from the hour the party arrived. */
class SunClockTest {
    @Test
    void noonIsTheOneAndMidnightIsTheFloor() {
        assertEquals(1.0, SunClock.sunshine(SunClock.NOON), 1e-9);
        assertEquals(SunClock.NIGHT_FLOOR, SunClock.sunshine(SunClock.MIDNIGHT), 1e-9);
        assertEquals(1.0, SunClock.height(SunClock.NOON), 1e-9);
        assertEquals(-1.0, SunClock.height(SunClock.MIDNIGHT), 1e-9);
        assertEquals(0.0, SunClock.height(0), 1e-9);
        assertEquals(0.0, SunClock.height(12_000), 1e-9);
    }

    @Test
    void theDayTimeWrapsAndNegativeTimesToo() {
        assertEquals(0L, SunClock.wrap(24_000));
        assertEquals(1L, SunClock.wrap(24_001));
        assertEquals(23_999L, SunClock.wrap(-1));
        assertEquals(SunClock.sunshine(6_000), SunClock.sunshine(6_000 + 24_000L * 1000), 1e-9);
        assertEquals(SunClock.sunshine(5_000), SunClock.sunshine(5_000 - 24_000L * 3), 1e-9);
    }

    @Test
    void theSunClimbsFromDawnToNoonAndFallsAgain() {
        double last = -1;
        for (int t = 18_000; t <= 24_000 + 6_000; t += 100) {
            double s = SunClock.sunshine(t);
            assertTrue(s >= last - 1e-12, "never falls before noon: t=" + t);
            last = s;
        }
        for (int t = 6_000; t <= 18_000; t += 100) {
            double s = SunClock.sunshine(t);
            assertTrue(s <= last + 1e-12, "never rises after noon: t=" + t);
            last = s;
        }
        for (int t = 0; t < 24_000; t += 50) {
            double s = SunClock.sunshine(t);
            assertTrue(s >= SunClock.NIGHT_FLOOR && s <= 1.0, "within bounds at " + t);
            assertEquals(SunClock.sunshine(6_000 - (t - 6_000)), s, 1e-9, "the day is symmetric about noon: " + t);
        }
    }

    @Test
    void theDarkHalfIsAboutFortySevenPercentOfTheDayAndFlat() {
        int dark = 0;
        for (int t = 0; t < 24_000; t++) if (SunClock.sunshine(t) <= SunClock.NIGHT_FLOOR + 1e-9) dark++;
        assertTrue(dark > 10_000 && dark < 11_200, "dark ticks: " + dark);
        assertEquals(SunClock.NIGHT_FLOOR, SunClock.sunshine(15_000), 1e-12);
        assertEquals(SunClock.NIGHT_FLOOR, SunClock.sunshine(21_000), 1e-12);
    }

    @Test
    void theOneIsAboutAFifthOfTheDayAround() {
        int one = 0;
        for (int t = 0; t < 24_000; t++) if (SunClock.sunshine(t) >= SunClock.THE_ONE) one++;
        assertTrue(one > 3_000 && one < 4_000, "ticks of The One: " + one);
        assertTrue(SunClock.sunshine(6_000 + 1_700) >= SunClock.THE_ONE);
        assertTrue(SunClock.sunshine(6_000 + 1_900) < SunClock.THE_ONE);
    }

    @Test
    void everyHourHasAPhaseAndTheOrderIsRight() {
        assertEquals(SunClock.Phase.NIGHT, SunClock.phaseAt(SunClock.MIDNIGHT));
        assertEquals(SunClock.Phase.THE_ONE, SunClock.phaseAt(SunClock.NOON));
        assertEquals(SunClock.Phase.DAWN, SunClock.phaseAt(1_000));
        assertEquals(SunClock.Phase.MORNING, SunClock.phaseAt(3_000));
        assertEquals(SunClock.Phase.AFTERNOON, SunClock.phaseAt(9_000));
        assertEquals(SunClock.Phase.DUSK, SunClock.phaseAt(11_500));
        assertTrue(SunClock.rising(1_000) && SunClock.rising(23_000) && !SunClock.rising(9_000) && !SunClock.rising(SunClock.NOON));
        for (SunClock.Phase p : SunClock.Phase.values()) assertFalse(p.label().isBlank());
    }

    @Test
    void theWorldFollowsUntilAFightStartsThenTheFightRunsFaster() {
        SunClock c = new SunClock();
        assertFalse(c.running());
        assertEquals(1234L, c.dayTime(1234, 99_999), "idle: the world's own time");
        c.start(SunClock.Start.WORLD, 4_000, 1_000, 3.0);
        assertTrue(c.running());
        assertEquals(4_000L, c.dayTime(4_000, 1_000));
        assertEquals(4_000L + 300, c.dayTime(500, 1_100), "100 game ticks are 300 sun ticks, whatever the world says now");
        assertEquals(SunClock.wrap(4_000 + 3 * 10_000), c.dayTime(0, 11_000));
        c.stop();
        assertEquals(777L, c.dayTime(777, 20_000), "the clock follows the world again");
    }

    @Test
    void theStartCanBeForcedAndTheSpeedIsCapped() {
        SunClock c = new SunClock();
        c.start(SunClock.Start.NOON, 18_000, 0, 0.0);
        assertEquals(6_000L, c.dayTime(18_000, 50_000), "speed 0 holds noon for ever");
        c.start(SunClock.Start.MIDNIGHT, 6_000, 0, 1.0);
        assertEquals(18_000L + 600, c.dayTime(6_000, 600));
        c.start(SunClock.Start.DAWN, 6_000, 0, 999.0);
        assertEquals(12.0, c.speed(), 0.0);
        c.start(SunClock.Start.WORLD, 23_990, 0, -4.0);
        assertEquals(0.0, c.speed(), 0.0);
        assertEquals(23_990L, c.dayTime(0, 5_000));
        c.start(null, 100, 0, 1.0);
        assertEquals(100L, c.dayTime(0, 0), "no start means the world's");
    }

    @Test
    void theStartIsParsedFromTheConfigString() {
        assertEquals(SunClock.Start.WORLD, SunClock.Start.parse("world"));
        assertEquals(SunClock.Start.NOON, SunClock.Start.parse(" Noon "));
        assertEquals(SunClock.Start.MIDNIGHT, SunClock.Start.parse("MIDNIGHT"));
        assertEquals(SunClock.Start.DAWN, SunClock.Start.parse("dawn"));
        assertEquals(SunClock.Start.WORLD, SunClock.Start.parse("dusk"));
        assertEquals(SunClock.Start.WORLD, SunClock.Start.parse(null));
    }

    @Test
    void aFightThatLastsSixMinutesAtTripleSpeedSeesMoreThanOneDay() {
        SunClock c = new SunClock();
        c.start(SunClock.Start.WORLD, 18_000, 0, 3.0);
        boolean sawNoon = false, sawNight = false;
        for (long g = 0; g <= 6 * 60 * 20; g += 20) {
            double s = c.sunshine(0, g);
            if (s >= SunClock.THE_ONE) sawNoon = true;
            if (s <= SunClock.NIGHT_FLOOR + 1e-9) sawNight = true;
        }
        assertTrue(sawNoon && sawNight, "a six-minute fight starting at midnight reaches The One");
    }
}
