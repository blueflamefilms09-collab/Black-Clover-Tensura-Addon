package com.newuniverse.nusmp.dungeon.nexus.collapse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The mutated guards of the collapse: when, how many, how strong, where. Everything is capped. */
class GuardPlanTest {
    @Test
    void theWavesComeInOrderAndNeverInTheLastThirtyFiveSeconds() {
        long last = -1;
        for (GuardPlan.Wave w : GuardPlan.WAVES) {
            assertTrue(w.at() > last, "ascending");
            assertTrue(w.at() >= 600, "none in the first half minute");
            assertTrue(CollapsePlan.TOTAL - w.at() >= 700, "none in the last 35 seconds, so the final dash is never blocked by a newcomer");
            assertTrue(w.perExit() >= 1 && w.perExit() <= 2);
            assertTrue(w.tier3Percent() >= 0 && w.tier3Percent() <= 100);
            last = w.at();
        }
        assertEquals(5, GuardPlan.WAVES.length);
        assertTrue(GuardPlan.WAVES[GuardPlan.WAVES.length - 1].tier3Percent() > GuardPlan.WAVES[0].tier3Percent(), "tougher later");
    }

    @Test
    void aWaveIsDueOnceAndOnlyTheNewestAfterAStall() {
        assertEquals(-1, GuardPlan.dueWave(0, 0));
        assertEquals(-1, GuardPlan.dueWave(0, 599));
        assertEquals(0, GuardPlan.dueWave(0, 600));
        assertEquals(-1, GuardPlan.dueWave(1, 600));
        assertEquals(1, GuardPlan.dueWave(1, 1500));
        assertEquals(4, GuardPlan.dueWave(0, 3000), "after a stall, the last one");
        assertEquals(-1, GuardPlan.dueWave(5, 99999));
        assertEquals(0, GuardPlan.dueWave(-3, 600));
        int next = 0, said = 0;
        for (long e = 0; e < CollapsePlan.TOTAL; e++) {
            int w = GuardPlan.dueWave(next, e);
            if (w >= 0) {
                assertEquals(GuardPlan.WAVES[w].at(), e);
                next = w + 1;
                said++;
            }
        }
        assertEquals(GuardPlan.WAVES.length, said);
    }

    @Test
    void theCountsAreHeldToEveryCap() {
        assertEquals(0, GuardPlan.count(-1, 4, 0, 0));
        assertEquals(0, GuardPlan.count(99, 4, 0, 0));
        assertEquals(0, GuardPlan.count(0, 0, 0, 0), "no exits, no guards");
        assertEquals(4, GuardPlan.count(0, 4, 0, 0));
        assertEquals(8, GuardPlan.count(2, 4, 0, 0));
        assertEquals(10 - 7, GuardPlan.count(2, 4, 7, 0), "never more than MAX_ALIVE together");
        assertEquals(0, GuardPlan.count(2, 4, 10, 0));
        assertEquals(0, GuardPlan.count(2, 4, 12, 0), "over the cap already: none, never negative");
        assertEquals(24 - 22, GuardPlan.count(2, 4, 0, 22), "never more than MAX_SPAWNED in all");
        assertEquals(0, GuardPlan.count(2, 4, 0, 30));
        int spawned = 0;
        for (int w = 0; w < GuardPlan.WAVES.length; w++) {
            int n = GuardPlan.count(w, 8, 0, spawned);
            assertTrue(n <= GuardPlan.MAX_ALIVE);
            spawned += n;
        }
        assertEquals(GuardPlan.MAX_SPAWNED, spawned, "eight exits would take it all and no more");
    }

    @Test
    void anExitHoldsOnlyAFewAtOnce() {
        assertTrue(GuardPlan.roomAtExit(0));
        assertTrue(GuardPlan.roomAtExit(GuardPlan.PER_EXIT_ALIVE - 1));
        assertFalse(GuardPlan.roomAtExit(GuardPlan.PER_EXIT_ALIVE));
        assertTrue(GuardPlan.PER_EXIT_ALIVE <= GuardPlan.MAX_ALIVE);
    }

    @Test
    void theTierFollowsTheRollAndTheWave() {
        assertEquals(2, GuardPlan.tier(0, 0), "an early wave is tier 2 whatever the roll");
        assertEquals(2, GuardPlan.tier(0, 99));
        assertEquals(3, GuardPlan.tier(4, 10), "a late wave is half tier 3");
        assertEquals(2, GuardPlan.tier(4, 80));
        assertEquals(2, GuardPlan.tier(-1, 0));
        assertEquals(2, GuardPlan.tier(40, 0));
        int tier3 = 0;
        for (int roll = 0; roll < 100; roll++) if (GuardPlan.tier(3, roll) == 3) tier3++;
        assertEquals(GuardPlan.WAVES[3].tier3Percent(), tier3);
    }

    @Test
    void theMutationIsFiniteAndNeverWeakensAnythingStronger() {
        for (int tier : new int[]{2, 3}) {
            GuardPlan.Mutation m = GuardPlan.mutation(tier);
            assertTrue(m.health() > 0 && m.health() <= 1.0 && m.attack() > 0 && m.attack() <= 0.5 && m.speed() > 0 && m.speed() <= 0.15);
            assertTrue(m.healthCap() >= 100 && m.attackCap() >= 10);
            assertEquals(0.0, GuardPlan.extra(m.healthCap() * 2, m.health(), m.healthCap()), "already past the cap: nothing is added");
            assertEquals(0.0, GuardPlan.extra(m.healthCap(), m.health(), m.healthCap()));
            double e = GuardPlan.extra(100.0, m.health(), m.healthCap());
            assertTrue(e > 0 && 100.0 + e <= m.healthCap());
        }
        assertTrue(GuardPlan.mutation(3).health() < GuardPlan.mutation(2).health(), "the bigger creature grows by less");
        assertEquals(40.0, GuardPlan.extra(100.0, 0.4, 400.0), 1e-9);
        assertEquals(20.0, GuardPlan.extra(100.0, 0.5, 120.0), 1e-9, "held to the cap");
        assertEquals(0.0, GuardPlan.extra(0.0, 0.5, 120.0));
        assertEquals(0.0, GuardPlan.extra(-5.0, 0.5, 120.0));
        assertEquals(0.0, GuardPlan.extra(10.0, -0.5, 120.0));
        assertEquals(0.0, GuardPlan.extra(10.0, 0.0, 120.0));
        assertEquals(GuardPlan.mutation(5).health(), GuardPlan.mutation(3).health(), "anything above tier 3 is tier 3");
    }

    @Test
    void aGuardStandsOnTheRingAroundItsExitAndNotTwiceInARow() {
        for (int exit = 0; exit < 8; exit++) {
            for (int n = 0; n < 10; n++) {
                int[] last = null;
                for (int attempt = 0; attempt < 6; attempt++) {
                    int[] o = GuardPlan.offset(exit, n, attempt);
                    int d = Math.max(Math.abs(o[0]), Math.abs(o[1]));
                    assertTrue(d >= GuardPlan.RING_MIN && d <= GuardPlan.RING_MAX, "on the ring: " + d);
                    assertArrayEquals(o, GuardPlan.offset(exit, n, attempt), "stable");
                    if (last != null) assertFalse(last[0] == o[0] && last[1] == o[1], "a new try is a new spot");
                    last = o;
                }
            }
        }
        assertTrue(GuardPlan.MIN_PLAYER_GAP >= 5.0 && GuardPlan.TELEGRAPH >= 20);
        int[] neg = GuardPlan.offset(-3, -5, -2);
        assertTrue(Math.max(Math.abs(neg[0]), Math.abs(neg[1])) >= GuardPlan.RING_MIN, "negative numbers are fine");
    }

    @Test
    void guardsAreHandedOutToTheExitsInTurnAndNeverPastACap() {
        int[] none = new int[4];
        int[] first = GuardPlan.assign(0, 4, none, 0, 0);
        assertArrayEquals(new int[]{0, 1, 2, 3}, first, "one for each exit in the first wave");
        int[] second = GuardPlan.assign(1, 4, none, 0, 0);
        assertArrayEquals(new int[]{1, 2, 3, 0}, second, "the start moves with the wave");
        int[] third = GuardPlan.assign(2, 4, none, 0, 0);
        assertEquals(8, third.length);
        int[] hist = new int[4];
        for (int e : third) hist[e]++;
        for (int h : hist) assertEquals(2, h, "two at each exit");
        int[] crowded = GuardPlan.assign(2, 4, new int[]{3, 0, 3, 1}, 4, 0);
        int[] h2 = new int[4];
        for (int e : crowded) h2[e]++;
        assertEquals(0, h2[0], "an exit that holds three takes no more");
        assertEquals(0, h2[2]);
        assertEquals(GuardPlan.PER_EXIT_ALIVE, h2[1], "the empty exit is filled to the cap");
        assertEquals(GuardPlan.PER_EXIT_ALIVE - 1, h2[3], "the exit that holds one takes two");
        assertEquals(5, crowded.length, "all the room there is: six were allowed, five fit");
    }

    @Test
    void theAssignmentRespectsTheTotalsAndSurvivesNonsense() {
        assertEquals(0, GuardPlan.assign(0, 0, new int[0], 0, 0).length, "no exits");
        assertEquals(0, GuardPlan.assign(-3, 4, new int[4], 10, 0).length, "ten alive already");
        assertEquals(0, GuardPlan.assign(0, 4, new int[4], 0, 24).length, "all made");
        assertEquals(2, GuardPlan.assign(2, 4, new int[4], 8, 0).length, "only what the cap leaves");
        assertEquals(4, GuardPlan.assign(0, 4, null, 0, 0).length, "no counts given: all zero");
        assertEquals(4, GuardPlan.assign(0, 4, new int[1], 0, 0).length, "counts too short: the rest zero");
        for (int wave = 0; wave < GuardPlan.WAVES.length; wave++) {
            for (int exits = 1; exits <= 8; exits++) {
                for (int alive = 0; alive <= GuardPlan.MAX_ALIVE; alive += 3) {
                    int[] a = GuardPlan.assign(wave, exits, new int[exits], alive, 0);
                    assertTrue(a.length <= GuardPlan.count(wave, exits, alive, 0));
                    assertTrue(alive + a.length <= GuardPlan.MAX_ALIVE);
                    int[] load = new int[exits];
                    for (int e : a) {
                        assertTrue(e >= 0 && e < exits);
                        load[e]++;
                        assertTrue(load[e] <= GuardPlan.PER_EXIT_ALIVE);
                    }
                }
            }
        }
    }
}
