package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: "the encounters never trigger" was, among other things, a claim about arithmetic: a d20 against a difficulty class of 17 with a light modifier of minus three
 * and a threat modifier of nothing (a party in the dark on the first floor) can only be lifted to the class by a natural 20. These tests put numbers on it: the
 * exact odds of every combination of the modifiers the game can produce, what the pity timer ({@link Tension}) does to the wait, and the properties that must hold
 * whatever the configuration (the odds are probabilities, a natural 1 is quiet, a natural 20 is never quiet, a party that waits is eventually met).
 */
class EncounterReachTest {
    private static final int DC = 17;

    private static EncounterRoll.Mods mods(int threat, int noise, int light, int party, int table) { return new EncounterRoll.Mods(threat, noise, light, party, table, 0); }

    @Test
    void theOddsAreProbabilitiesForEveryModifierTheGameCanProduce() {
        for (int threat = 0; threat <= 5; threat++)
            for (int noise = 0; noise <= 4; noise++)
                for (int light = -3; light <= 3; light++)
                    for (int party = 0; party <= 2; party++)
                        for (int table = -3; table <= 3; table++)
                            for (int dc : new int[] {5, 12, 17, 24, 30}) {
                                double[] d = EncounterRoll.distribution(mods(threat, noise, light, party, table), dc);
                                double sum = 0;
                                for (double p : d) {
                                    assertTrue(p >= 0.0 && p <= 1.0);
                                    sum += p;
                                }
                                assertEquals(1.0, sum, 1e-9);
                                assertTrue(d[Band.NOTHING.ordinal()] >= 0.05 - 1e-9, "a natural 1 is always quiet");
                                assertTrue(EncounterRoll.chance(mods(threat, noise, light, party, table), dc) >= 0.05 - 1e-9, "a natural 20 is never quiet: some trouble is always possible");
                            }
    }

    @Test
    void aSoloistInTheDarkOnTheFirstFloorIsMetByAnythingOnlyOnANaturalTwentyAtTheShippedDc() {
        // the case of the report: light minus three, threat of floor one nothing, no noise, a table with no bonus
        EncounterRoll.Mods dark = mods(EncounterRoll.threatMod(1, 5), 0, LightRules.mod(0), 0, 0);
        assertEquals(-3, dark.total());
        assertEquals(0.05, EncounterRoll.chance(dark, DC), 1e-9, "one pass in twenty");
        double without = Tension.expectedPasses(dark, DC, 1, 0, 2000);
        assertEquals(20.0, without, 0.5, "twenty passes, a hundred seconds at the default interval, without the pity timer");
        double with = Tension.expectedPasses(dark, DC, Tension.DEFAULT_EVERY, Tension.DEFAULT_CAP, 2000);
        assertTrue(with < without * 0.5, "the pity timer more than halves the wait: " + with + " against " + without);
        assertTrue(with * 100 / 20.0 <= 60.0, "and the first encounter comes within about a minute on average: " + with * 100 / 20.0 + " s");
    }

    @Test
    void theWorstCaseTheTablesAndTheLightCanMakeIsStillMetWithinAboutTwoMinutes() {
        // light minus three AND a table bonus of minus three (the quietest theme), floor one, a soloist: the lowest total the game can produce
        EncounterRoll.Mods worst = mods(0, 0, -3, 0, -3);
        assertEquals(-6, worst.total());
        double passes = Tension.expectedPasses(worst, DC, Tension.DEFAULT_EVERY, Tension.DEFAULT_CAP, 5000);
        assertTrue(passes <= 30.0, "expected passes " + passes);
        assertTrue(passes * 100 / 20.0 <= 150.0, "within two and a half minutes on average: " + passes * 5 + " s");
        // with the timer off it is much worse, which is what the owner saw
        assertTrue(Tension.expectedPasses(worst, DC, 1, 0, 5000) >= 19.9);
    }

    @Test
    void theTensionNeverMakesAnyRollCertainAndNeverBeatsTheDiceCompletely() {
        for (int light = -3; light <= 3; light++)
            for (int table = -3; table <= 3; table++) {
                EncounterRoll.Mods m = mods(0, 0, light, 0, table).withTension(Tension.DEFAULT_CAP);
                assertTrue(EncounterRoll.chance(m, DC) < 1.0, "a natural 1 stays quiet with the whole pity bonus");
                assertTrue(EncounterRoll.chance(m, DC) > 0.0);
            }
        assertEquals(0.0, EncounterRoll.chance(mods(0, 0, 0, 0, 0), 30) - EncounterRoll.chance(mods(0, 0, 0, 0, 0), 30), 0.0);
    }

    @Test
    void theBonusGrowsOnePointEveryTwoQuietPassesAndStopsAtTheCap() {
        assertEquals(0, Tension.bonus(0, 2, 5));
        assertEquals(0, Tension.bonus(1, 2, 5));
        assertEquals(1, Tension.bonus(2, 2, 5));
        assertEquals(1, Tension.bonus(3, 2, 5));
        assertEquals(2, Tension.bonus(4, 2, 5));
        assertEquals(5, Tension.bonus(10, 2, 5));
        assertEquals(5, Tension.bonus(1_000_000, 2, 5));
        assertEquals(0, Tension.bonus(10, 2, 0), "cap 0 switches it off");
        assertEquals(0, Tension.bonus(10, 0, 5), "a bad interval switches it off instead of dividing by zero");
        assertEquals(0, Tension.bonus(-4, 2, 5));
        int prev = 0;
        for (int q = 0; q < 100; q++) {
            int b = Tension.bonus(q, 2, 5);
            assertTrue(b >= prev && b <= 5);
            prev = b;
        }
    }

    @Test
    void theQuietCounterSaturatesInsteadOfOverflowing() {
        int q = Tension.MAX_QUIET - 2;
        for (int i = 0; i < 10; i++) q = Tension.quiet(q);
        assertEquals(Tension.MAX_QUIET, q);
        assertEquals(1, Tension.quiet(-5));
        assertEquals(1, Tension.quiet(0));
    }

    @Test
    void aBrightLoudPartyDeepDownIsMetAlmostEveryPassAndTheTensionAddsNothingItNeeds() {
        EncounterRoll.Mods hot = mods(5, 4, 3, 2, 0);
        assertTrue(EncounterRoll.chance(hot, DC) >= 0.9, "" + EncounterRoll.chance(hot, DC));
        assertTrue(EncounterRoll.distribution(hot, DC)[Band.AMBUSH.ordinal()] >= 0.5);
    }

    @Test
    void theDiceGateSaysWhatTheOddsAre() {
        EncounterRoll.Mods dark = mods(0, 0, -3, 0, 0);
        EncGates.Gate g = EncGates.dice(dark, DC, 100, Tension.DEFAULT_EVERY, Tension.DEFAULT_CAP);
        assertEquals(EncGates.Status.PASS, g.status());
        assertTrue(g.value().contains("-3 against DC 17"), g.value());
        assertTrue(g.value().contains("5% of passes"), g.value());
        EncGates.Gate off = EncGates.dice(dark, DC, 100, 1, 0);
        assertEquals(EncGates.Status.WARN, off.status(), "with the timer off the first encounter is minutes away: " + off.value());
    }
}
