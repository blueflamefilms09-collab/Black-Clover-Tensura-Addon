package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the meter has to forget (a level that only rose would end every expedition in a siege) and has to saturate (a long fight must not break the dice). These
 * tests pin the decay, the ceiling, the distance falloff, the reading as a modifier and the refusal of bad amounts.
 */
class NoiseMeterTest {
    private static final double DECAY = 0.2;

    @Test
    void aFreshMeterReadsZero() {
        NoiseMeter m = new NoiseMeter();
        assertEquals(0.0, m.level(100, DECAY), 0.0);
        assertEquals(0.0, m.peek(100, DECAY), 0.0);
    }

    @Test
    void eventsAddUp() {
        NoiseMeter m = new NoiseMeter();
        m.add(1.0, 100, DECAY);
        m.add(2.0, 100, DECAY);
        assertEquals(3.0, m.level(100, DECAY), 1e-9);
    }

    @Test
    void theLevelLosesTheConfiguredFractionEverySecond() {
        NoiseMeter m = new NoiseMeter();
        m.add(10.0, 0, DECAY);
        assertEquals(8.0, m.level(20, DECAY), 1e-9, "one second: a fifth is gone");
        assertEquals(6.4, m.level(40, DECAY), 1e-9);
        assertEquals(6.4 * Math.pow(0.8, 0.5), m.level(50, DECAY), 1e-9, "half a second is the square root of a second");
    }

    @Test
    void peekDoesNotChangeTheMeterButLevelDoes() {
        NoiseMeter m = new NoiseMeter();
        m.add(10.0, 0, DECAY);
        assertEquals(8.0, m.peek(20, DECAY), 1e-9);
        assertEquals(8.0, m.peek(20, DECAY), 1e-9);
        assertEquals(10.0, m.peek(0, DECAY), 1e-9);
        m.level(20, DECAY);
        assertEquals(8.0, m.peek(20, DECAY), 1e-9);
    }

    @Test
    void anOldNoiseIsForgottenCompletely() {
        NoiseMeter m = new NoiseMeter();
        m.add(10.0, 0, DECAY);
        assertEquals(0.0, m.level(20 * 120, DECAY), 0.0);
    }

    @Test
    void theLevelIsCappedSoALongFightCannotBreakTheScale() {
        NoiseMeter m = new NoiseMeter();
        for (int i = 0; i < 100; i++) m.add(6.0, 0, DECAY);
        assertEquals(NoiseMeter.CEILING, m.level(0, DECAY), 1e-9);
    }

    @Test
    void badAmountsAddNothing() {
        NoiseMeter m = new NoiseMeter();
        m.add(-5.0, 0, DECAY);
        m.add(Double.NaN, 0, DECAY);
        m.add(Double.POSITIVE_INFINITY, 0, DECAY);
        assertEquals(0.0, m.level(0, DECAY), 0.0);
    }

    @Test
    void theDecayRateIsClampedToAFractionAndFullDecayEmptiesAtOnce() {
        assertEquals(0.0, NoiseMeter.decayed(5.0, 20, 1.0), 0.0);
        assertEquals(5.0, NoiseMeter.decayed(5.0, 20, 0.0), 1e-9, "a zero rate never forgets");
        assertEquals(5.0, NoiseMeter.decayed(5.0, 20, -3.0), 1e-9);
        assertEquals(0.0, NoiseMeter.decayed(5.0, 20, 7.0), 0.0);
        assertEquals(5.0, NoiseMeter.decayed(5.0, 0, 0.5), 1e-9);
        assertEquals(0.0, NoiseMeter.decayed(-1.0, 20, 0.5), 0.0);
    }

    @Test
    void clearEmptiesTheMeter() {
        NoiseMeter m = new NoiseMeter();
        m.add(9.0, 0, DECAY);
        m.clear();
        assertEquals(0.0, m.level(0, DECAY), 0.0);
        m.add(1.0, 5, DECAY);
        assertEquals(1.0, m.level(5, DECAY), 1e-9);
    }

    @Test
    void distanceFadesAnEventQuadraticallyAndItIsGoneAtTheEarshot() {
        assertEquals(1.0, NoiseMeter.falloff(0, 20), 1e-9);
        assertEquals(0.25, NoiseMeter.falloff(10, 20), 1e-9);
        assertEquals(0.0, NoiseMeter.falloff(20, 20), 0.0);
        assertEquals(0.0, NoiseMeter.falloff(35, 20), 0.0);
        assertEquals(0.0, NoiseMeter.falloff(5, 0), 0.0);
        assertEquals(1.0, NoiseMeter.falloff(-3, 20), 1e-9, "a negative distance counts as zero");
        assertTrue(NoiseMeter.falloff(4, 20) > NoiseMeter.falloff(8, 20));
    }

    @Test
    void theReadingBecomesAModifierOfOnePerTwoAndAHalfUpToTheCap() {
        assertEquals(0, NoiseMeter.mod(0.0, 4));
        assertEquals(0, NoiseMeter.mod(2.49, 4));
        assertEquals(1, NoiseMeter.mod(2.5, 4));
        assertEquals(1, NoiseMeter.mod(3.0, 4), "a steady sprint");
        assertEquals(2, NoiseMeter.mod(5.0, 4));
        assertEquals(3, NoiseMeter.mod(8.0, 4));
        assertEquals(4, NoiseMeter.mod(10.0, 4), "a fight");
        assertEquals(4, NoiseMeter.mod(40.0, 4));
        assertEquals(2, NoiseMeter.mod(40.0, 2));
        assertEquals(0, NoiseMeter.mod(40.0, 0));
        assertEquals(0, NoiseMeter.mod(Double.NaN, 4));
        assertEquals(0, NoiseMeter.mod(-5.0, 4));
    }

    @Test
    void aSteadySprintSettlesNearThreeAndAFightSaturatesTheScale() {
        // simulate 60 s: sprint steps are 3.3 a second at 0.2; a brawl is 3.3 hurt events a second at 2.0 (inflow 6.7 a second against a decay rate of 0.22 a second)
        NoiseMeter sprint = new NoiseMeter(), fight = new NoiseMeter();
        long now = 0;
        for (int i = 0; i < 60 * 20; i++, now++) {
            if (i % 6 == 0) sprint.add(0.2, now, DECAY);
            if (i % 6 == 0) fight.add(2.0, now, DECAY);
        }
        double s = sprint.level(now, DECAY), f = fight.level(now, DECAY);
        assertTrue(s > 2.0 && s < 4.5, "sprint " + s);
        assertTrue(f > 10.0 && f <= NoiseMeter.CEILING, "fight " + f);
        assertEquals(1, NoiseMeter.mod(s, 4));
        assertEquals(4, NoiseMeter.mod(f, 4));
        // ten quiet seconds later the fight has faded to a lower modifier
        double later = fight.level(now + 200, DECAY);
        assertTrue(later < f * 0.2, "after ten quiet seconds " + later);
    }
}
