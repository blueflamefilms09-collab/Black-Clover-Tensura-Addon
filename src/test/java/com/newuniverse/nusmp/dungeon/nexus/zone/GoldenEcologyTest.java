package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.zone.GoldenEcology.Act;
import com.newuniverse.nusmp.dungeon.nexus.zone.GoldenEcology.Census;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The balance of the Golden City's wildlife: more apex predators than anywhere else, fed by herds, never over a cap, brought back slowly. */
class GoldenEcologyTest {
    private static final long FAR = Long.MAX_VALUE / 4;

    @Test
    void theGardenHoldsMoreApexPredatorsThanTheRestOfTheDungeonAndStillHasACap() {
        assertTrue(GoldenEcology.APEX_TARGET > 2, "more than the two a chunk of the rest of the Dungeon allows are aimed at");
        assertTrue(GoldenEcology.APEX_CAP > GoldenEcology.APEX_TARGET);
        assertTrue(GoldenEcology.PREY_CAP > GoldenEcology.PREY_TARGET && GoldenEcology.PREY_TARGET > GoldenEcology.PREY_FLOOR);
        assertEquals(1, GoldenEcology.LEGEND_CAP);
    }

    @Test
    void preyWantedGrowsWithThePredatorsAndStaysInItsRange() {
        int last = 0;
        for (int apex = 0; apex <= 20; apex++) {
            int w = GoldenEcology.preyWanted(apex);
            assertTrue(w >= GoldenEcology.PREY_FLOOR && w <= GoldenEcology.PREY_TARGET);
            assertTrue(w >= last);
            last = w;
        }
        assertEquals(GoldenEcology.PREY_FLOOR, GoldenEcology.preyWanted(0));
        assertEquals(GoldenEcology.PREY_TARGET, GoldenEcology.preyWanted(20));
    }

    @Test
    void decisionsFollowTheFoodWeb() {
        // an empty garden gets prey first, never a predator before there is something to hunt
        assertEquals(Act.PREY, GoldenEcology.decide(new Census(0, 0, 0), FAR, FAR, FAR, false, 0.5));
        assertEquals(Act.NONE, GoldenEcology.decide(new Census(0, 0, 0), 0, FAR, FAR, false, 0.5), "but only slowly: the clock has not run out");
        // with a herd the first predator may come
        assertEquals(Act.APEX, GoldenEcology.decide(new Census(GoldenEcology.preyWanted(0), 0, 0), FAR, FAR, FAR, false, 0.5));
        assertEquals(Act.NONE, GoldenEcology.decide(new Census(GoldenEcology.preyWanted(0), 0, 0), FAR, 0, FAR, false, 0.5), "a predator only every half minute");
        // a predator needs three prey to live on and a few more
        assertEquals(Act.PREY, GoldenEcology.decide(new Census(5, 2, 0), FAR, FAR, FAR, false, 0.5), "short of prey: prey first");
        assertEquals(Act.NONE, GoldenEcology.decide(new Census(GoldenEcology.preyWanted(GoldenEcology.APEX_TARGET), GoldenEcology.APEX_TARGET, 0), FAR, FAR, FAR, false, 0.5), "balanced: nothing to do");
        // too many: cull, whatever else is due
        assertEquals(Act.CULL_APEX, GoldenEcology.decide(new Census(10, GoldenEcology.APEX_CAP + 1, 0), FAR, FAR, FAR, true, 0.0));
        assertEquals(Act.CULL_PREY, GoldenEcology.decide(new Census(GoldenEcology.PREY_CAP + 1, 2, 0), FAR, FAR, FAR, true, 0.0));
        // a legendary creature: only in a thriving garden, with the roster to give one, rarely
        Census thriving = new Census(20, 4, 0);
        assertEquals(Act.LEGEND, GoldenEcology.decide(thriving, 0, 0, FAR, true, 0.1));
        assertNotEquals(Act.LEGEND, GoldenEcology.decide(thriving, 0, 0, FAR, true, 0.9), "the roll says not now");
        assertNotEquals(Act.LEGEND, GoldenEcology.decide(thriving, 0, 0, FAR, false, 0.1), "no legend in the roster");
        assertNotEquals(Act.LEGEND, GoldenEcology.decide(thriving, 0, 0, GoldenEcology.LEGEND_EVERY - 1, true, 0.1), "too soon after the last");
        assertNotEquals(Act.LEGEND, GoldenEcology.decide(new Census(20, 4, 1), 0, 0, FAR, true, 0.1), "only one at a time");
        assertNotEquals(Act.LEGEND, GoldenEcology.decide(new Census(20, 2, 0), 0, 0, FAR, true, 0.1), "not before the apex predators have settled");
        assertNotEquals(Act.LEGEND, GoldenEcology.decide(new Census(10, 4, 0), 0, 0, FAR, true, 0.1), "not in a poor garden");
    }

    @Test
    void aClockThatWentBackwardsOrNeverRanCountsAsLongAgo() {
        assertTrue(GoldenEcology.since(100, Long.MIN_VALUE) > GoldenEcology.LEGEND_EVERY);
        assertTrue(GoldenEcology.since(100, 500) > GoldenEcology.LEGEND_EVERY, "another world's clock");
        assertEquals(40, GoldenEcology.since(140, 100));
    }

    /**
     * Five hours of the garden, a pass every five seconds: the predators hunt (each takes a prey about once in a minute and a half), the
     * herd breeds a little by itself, and the garden tops both up as the rules allow. The caps hold, the herd never dies out for long, the
     * births are slow, and the predators do settle in.
     */
    @Test
    void aWholeDayOfTheGardenStaysWithinItsCapsAndNeverEmptiesForLong() {
        Random rnd = new Random(7);
        int prey = 0, apex = 0, legend = 0, maxPrey = 0, maxApex = 0, emptyStreak = 0, longestEmpty = 0, preyBirths = 0, apexBirths = 0, legendBirths = 0, apexLost = 0;
        long lastPrey = Long.MIN_VALUE, lastApex = Long.MIN_VALUE, lastLegend = Long.MIN_VALUE;
        final long pass = 100;
        final long hours = 5;
        for (long t = 0; t < hours * 72_000; t += pass) {
            // nature: predators hunt, the herd breeds a little, a hungry predator with no prey left dies slowly
            for (int i = 0; i < apex + legend && prey > 0; i++) if (rnd.nextInt(18) == 0) prey--;
            if (prey > 0 && prey < GoldenEcology.PREY_TARGET && rnd.nextInt(40) == 0) prey++;
            if (apex > 0 && prey < apex * 2 && rnd.nextInt(60) == 0) {
                apex--;
                apexLost++;
            }
            Act a = GoldenEcology.decide(new Census(prey, apex, legend), GoldenEcology.since(t, lastPrey), GoldenEcology.since(t, lastApex), GoldenEcology.since(t, lastLegend), true, rnd.nextDouble());
            switch (a) {
                case PREY -> { prey++; lastPrey = t; preyBirths++; }
                case APEX -> { apex++; lastApex = t; apexBirths++; }
                case LEGEND -> { legend++; lastLegend = t; legendBirths++; }
                case CULL_APEX -> apex--;
                case CULL_PREY -> prey--;
                default -> { }
            }
            maxPrey = Math.max(maxPrey, prey);
            maxApex = Math.max(maxApex, apex);
            assertTrue(legend <= GoldenEcology.LEGEND_CAP);
            emptyStreak = prey == 0 && t > 20 * 60 * 20L ? emptyStreak + 1 : 0;
            longestEmpty = Math.max(longestEmpty, emptyStreak);
        }
        assertTrue(maxApex <= GoldenEcology.APEX_CAP, "apex predators: " + maxApex);
        assertTrue(maxPrey <= GoldenEcology.PREY_CAP, "prey: " + maxPrey);
        assertTrue(maxApex >= 3, "the predators do settle in: " + maxApex);
        assertTrue(longestEmpty <= GoldenEcology.PREY_EVERY / pass + 2, "the herd is never gone for long: " + longestEmpty + " passes in a row");
        assertTrue(preyBirths <= hours * 72_000 / GoldenEcology.PREY_EVERY + 1, "prey births are slow: " + preyBirths);
        assertTrue(apexBirths <= hours * 72_000 / GoldenEcology.APEX_EVERY + 1, "apex births are slow: " + apexBirths + " (" + apexLost + " lost)");
        assertTrue(legendBirths <= hours * 72_000 / GoldenEcology.LEGEND_EVERY + 1);
    }

    @Test
    void aPlaceIsPickedClearOfPlayersAndGates() {
        int[][] places = {{0, -50, 0}, {10, -50, 0}, {40, -50, 0}, {100, -50, 0}};
        double[][] players = {{1, 1}};
        double[][] gates = {{100.5, 0.5}};
        assertEquals(2, GoldenEcology.pick(places, players, gates, 24, 14, 0.0), "the first two are too near the player, so the third");
        assertEquals(2, GoldenEcology.pick(places, players, gates, 24, 14, 0.4));
        assertEquals(2, GoldenEcology.pick(places, players, gates, 24, 14, 0.99), "the last is at a gate: wraps round");
        assertEquals(-1, GoldenEcology.pick(places, new double[][]{{40, 0}, {0, 0}, {100, 0}}, gates, 24, 14, 0.5), "nowhere is clear");
        assertEquals(0, GoldenEcology.pick(places, new double[0][], new double[0][], 24, 14, 0.0), "nobody about: the first");
        assertEquals(-1, GoldenEcology.pick(new int[0][], players, gates, 24, 14, 0.5));
        assertEquals(3, GoldenEcology.pick(places, new double[0][], new double[0][], 24, 14, 1.5), "a roll out of range is clamped");
    }

    @Test
    void onlyACreatureFarFromEverybodyIsTakenAway() {
        double far = GoldenEcology.CULL_CLEAR * GoldenEcology.CULL_CLEAR;
        assertEquals(-1, GoldenEcology.cull(new double[]{1.0, far - 1, 100.0}), "all within sight");
        assertEquals(1, GoldenEcology.cull(new double[]{far, far * 4, far * 2}), "the farthest of the far");
        assertEquals(-1, GoldenEcology.cull(new double[0]));
    }
}
