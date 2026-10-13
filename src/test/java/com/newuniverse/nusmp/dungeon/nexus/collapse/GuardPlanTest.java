package com.newuniverse.nusmp.dungeon.nexus.collapse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The frenzied horde of the collapse: when, how many, split between the exits and the players, how strong, where. Everything is capped. */
class GuardPlanTest {
    private static int[] histogram(int[] plan, int exits) {
        int[] h = new int[exits + 1];                                   // the last cell counts the player's
        for (int slot : plan) h[slot == GuardPlan.PLAYER ? exits : slot]++;
        return h;
    }

    // ---------------------------------------------------------------- the waves
    @Test
    void sixWavesGrowingTheFirstAtTwentySecondsAndNoneInTheLastThirtyFive() {
        assertEquals(6, GuardPlan.WAVES.length);
        assertEquals(400L, GuardPlan.WAVES[0].at(), "the first wave at +20 s");
        long last = -1;
        int[] perExit = new int[GuardPlan.WAVES.length];
        for (int i = 0; i < GuardPlan.WAVES.length; i++) {
            GuardPlan.Wave w = GuardPlan.WAVES[i];
            assertTrue(w.at() > last, "ascending");
            assertTrue(w.at() >= 400, "none in the first twenty seconds");
            assertTrue(CollapsePlan.TOTAL - w.at() >= 700, "none in the last 35 seconds, so the final dash is never blocked by a newcomer");
            assertTrue(w.perExit() >= 2 && w.perExit() <= 4);
            assertTrue(w.tier3Percent() >= 0 && w.tier3Percent() <= 100);
            perExit[i] = w.perExit();
            last = w.at();
        }
        assertArrayEquals(new int[]{2, 2, 3, 3, 4, 3}, perExit, "the sizes grow, the last wave eases off");
        assertTrue(GuardPlan.WAVES[GuardPlan.WAVES.length - 1].tier3Percent() > GuardPlan.WAVES[0].tier3Percent(), "tougher later");
        assertTrue(perExit[4] > perExit[0], "bigger later");
    }

    @Test
    void aWaveIsDueOnceAndOnlyTheNewestAfterAStall() {
        assertEquals(-1, GuardPlan.dueWave(0, 0));
        assertEquals(-1, GuardPlan.dueWave(0, 399));
        assertEquals(0, GuardPlan.dueWave(0, 400));
        assertEquals(-1, GuardPlan.dueWave(1, 400));
        assertEquals(1, GuardPlan.dueWave(1, 1000));
        assertEquals(5, GuardPlan.dueWave(0, 3000), "after a stall, the last one");
        assertEquals(-1, GuardPlan.dueWave(6, 99999));
        assertEquals(0, GuardPlan.dueWave(-3, 400));
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
    void theHordeKeepsItsOwnCeilingsAndTheWallsCrackForTwoSeconds() {
        assertEquals(24, GuardPlan.MAX_ALIVE);
        assertEquals(6, GuardPlan.PER_EXIT_ALIVE);
        assertEquals(56, GuardPlan.MAX_SPAWNED);
        assertEquals(40, GuardPlan.TELEGRAPH, "the wall cracks for 40 ticks");
        assertTrue(GuardPlan.PER_EXIT_ALIVE <= GuardPlan.MAX_ALIVE && GuardPlan.MAX_ALIVE <= GuardPlan.MAX_SPAWNED);
        assertTrue(GuardPlan.MIN_PLAYER_GAP >= 5.0);
        assertTrue(GuardPlan.EXIT_MIN < GuardPlan.EXIT_MAX && GuardPlan.PLAYER_MIN < GuardPlan.PLAYER_MAX && GuardPlan.PLAYER_MIN > GuardPlan.MIN_PLAYER_GAP, "a birth round a player is never at their feet");
        assertEquals(-1, GuardPlan.PLAYER, "exits are numbered from 0");
    }

    // ---------------------------------------------------------------- how many
    @Test
    void thePlayerGetsAboutHalfOfWhatAnExitGets() {
        assertEquals(1, GuardPlan.playerShare(0));
        assertEquals(1, GuardPlan.playerShare(1));
        assertEquals(2, GuardPlan.playerShare(2), "three at an exit, two round the player: half, rounded up");
        assertEquals(2, GuardPlan.playerShare(3));
        assertEquals(2, GuardPlan.playerShare(4));
        assertEquals(2, GuardPlan.playerShare(5));
        assertEquals(0, GuardPlan.playerShare(-1));
        assertEquals(0, GuardPlan.playerShare(6));
        for (int w = 0; w < GuardPlan.WAVES.length; w++) {
            int each = GuardPlan.WAVES[w].perExit();
            assertTrue(GuardPlan.playerShare(w) * 2 >= each && GuardPlan.playerShare(w) * 2 <= each + 1, "about half of " + each);
        }
    }

    @Test
    void theCountsAreHeldToEveryCap() {
        assertEquals(0, GuardPlan.count(-1, 4, true, 0, 0));
        assertEquals(0, GuardPlan.count(99, 4, true, 0, 0));
        assertEquals(1, GuardPlan.count(0, 0, true, 0, 0), "no exits: only the player's share");
        assertEquals(0, GuardPlan.count(0, 0, false, 0, 0), "no exits and nobody to follow: nobody");
        assertEquals(8, GuardPlan.count(0, 4, false, 0, 0));
        assertEquals(9, GuardPlan.count(0, 4, true, 0, 0));
        assertEquals(14, GuardPlan.count(2, 4, true, 0, 0));
        assertEquals(24 - 20, GuardPlan.count(2, 4, true, 20, 0), "never more than MAX_ALIVE together");
        assertEquals(0, GuardPlan.count(2, 4, true, 24, 0));
        assertEquals(0, GuardPlan.count(2, 4, true, 30, 0), "over the cap already: none, never negative");
        assertEquals(56 - 50, GuardPlan.count(2, 4, true, 0, 50), "never more than MAX_SPAWNED in all");
        assertEquals(0, GuardPlan.count(2, 4, true, 0, 60));
        assertEquals(0, GuardPlan.count(2, 4, true, Integer.MIN_VALUE, Integer.MAX_VALUE), "nonsense does not overflow into a flood");
        assertEquals(GuardPlan.wanted(2, 4, true), GuardPlan.count(2, 4, true, 0, 0));
        assertEquals(0, GuardPlan.wanted(2, -3, false), "a negative number of exits is none");
    }

    @Test
    void anExitHoldsOnlyAFewAtOnce() {
        assertTrue(GuardPlan.roomAtExit(0));
        assertTrue(GuardPlan.roomAtExit(GuardPlan.PER_EXIT_ALIVE - 1));
        assertFalse(GuardPlan.roomAtExit(GuardPlan.PER_EXIT_ALIVE));
    }

    // ---------------------------------------------------------------- the split between the exits and the player
    @Test
    void aWaveIsSplitBetweenTheExitsAndTheOnePlayerAndTheyAlternate() {
        int[] plan = GuardPlan.assign(0, 3, new int[3], true, 0, 0, 0);
        assertArrayEquals(new int[]{0, GuardPlan.PLAYER, 1, 2, 0, 1, 2}, plan, "two for each of three exits, one round the player, alternating");
        int[] h = histogram(plan, 3);
        assertArrayEquals(new int[]{2, 2, 2, 1}, h);
        int[] noPlayer = GuardPlan.assign(0, 3, new int[3], false, 0, 0, 0);
        assertArrayEquals(new int[]{2, 2, 2, 0}, histogram(noPlayer, 3), "nobody to follow, no share");
        int[] later = GuardPlan.assign(4, 2, new int[2], true, 0, 0, 0);
        assertArrayEquals(new int[]{4, 4, 2}, histogram(later, 2), "wave five: four at each exit and two round the player");
        int[] noExits = GuardPlan.assign(2, 0, new int[0], true, 0, 0, 0);
        assertArrayEquals(new int[]{GuardPlan.PLAYER, GuardPlan.PLAYER}, noExits, "no exit stands: the player's share alone");
    }

    @Test
    void theStartMovesWithTheWaveSoNoExitIsFavoured() {
        int[] none = new int[4];
        assertArrayEquals(new int[]{0, 1, 2, 3, 0, 1, 2, 3}, GuardPlan.assign(0, 4, none, false, 0, 0, 0));
        assertArrayEquals(new int[]{1, 2, 3, 0, 1, 2, 3, 0}, GuardPlan.assign(1, 4, none, false, 0, 0, 0), "the start moves with the wave");
        int[] third = GuardPlan.assign(2, 4, none, false, 0, 0, 0);
        assertEquals(12, third.length);
        for (int h : histogram(third, 4)) assertTrue(h == 3 || h == 0, "three at each exit");
    }

    @Test
    void anExitOrAPlayerAtTheirCeilingTakeNoMore() {
        int[] crowded = GuardPlan.assign(2, 4, new int[]{6, 0, 6, 4}, true, 0, 0, 0);
        int[] h = histogram(crowded, 4);
        assertEquals(0, h[0], "an exit that holds six takes no more");
        assertEquals(0, h[2]);
        assertEquals(GuardPlan.PER_EXIT_ALIVE, h[1], "the empty exit is filled to its ceiling");
        assertEquals(GuardPlan.PER_EXIT_ALIVE - 4, h[3], "the exit that holds four takes two");
        assertEquals(2, h[4], "the player's share is whole");
        assertEquals(10, crowded.length, "all the room there is: fourteen were wanted, ten fit");
        int[] busyPlayer = GuardPlan.assign(2, 2, new int[2], true, 5, 0, 0);
        assertArrayEquals(new int[]{3, 3, 1}, histogram(busyPlayer, 2), "a player with five near takes one more, not two");
        int[] full = GuardPlan.assign(2, 2, new int[2], true, GuardPlan.PER_EXIT_ALIVE, 0, 0);
        assertEquals(0, histogram(full, 2)[2], "a player with six near takes none");
        assertEquals(0, GuardPlan.assign(2, 3, new int[]{6, 6, 6}, false, 0, 0, 0).length, "every exit full: nothing");
    }

    @Test
    void whenACeilingCutsTheWaveShortBothTheExitsAndThePlayerStillGetSome() {
        int[] cut = GuardPlan.assign(2, 4, new int[4], true, 0, 21, 0);
        assertArrayEquals(new int[]{2, GuardPlan.PLAYER, 3}, cut, "three places left: an exit, the player, an exit");
        int[] tight = GuardPlan.assign(3, 4, new int[4], true, 0, 23, 0);
        assertEquals(1, tight.length);
        assertTrue(tight[0] >= 0, "one place: an exit first");
        int[] spawnedCut = GuardPlan.assign(0, 2, new int[2], true, 0, 0, 54);
        assertArrayEquals(new int[]{0, GuardPlan.PLAYER}, spawnedCut, "MAX_SPAWNED leaves two places: an exit and the player");
    }

    @Test
    void theAssignmentRespectsEveryCapAndSurvivesNonsense() {
        assertEquals(0, GuardPlan.assign(0, 0, new int[0], false, 0, 0, 0).length, "no exits, nobody");
        assertEquals(0, GuardPlan.assign(-3, 4, new int[4], true, 0, 0, 0).length, "no such wave");
        assertEquals(0, GuardPlan.assign(9, 4, new int[4], true, 0, 0, 0).length, "no such wave");
        assertEquals(0, GuardPlan.assign(0, 4, new int[4], true, 0, 24, 0).length, "all places taken");
        assertEquals(0, GuardPlan.assign(0, 4, new int[4], true, 0, 0, 56).length, "all made");
        assertEquals(9, GuardPlan.assign(0, 4, null, true, 0, 0, 0).length, "no counts given: all zero");
        assertEquals(9, GuardPlan.assign(0, 4, new int[1], true, 0, 0, 0).length, "counts too short: the rest zero");
        assertEquals(9, GuardPlan.assign(0, 4, new int[4], true, -7, 0, 0).length, "a negative count near the player is none");
        assertEquals(0, GuardPlan.assign(0, -2, null, false, 0, 0, 0).length, "a negative number of exits is none");
        for (int wave = 0; wave < GuardPlan.WAVES.length; wave++) {
            for (int exits = 0; exits <= 8; exits++) {
                for (boolean player : new boolean[]{false, true}) {
                    for (int alive = 0; alive <= GuardPlan.MAX_ALIVE + 2; alive += 3) {
                        for (int spawned = 0; spawned <= GuardPlan.MAX_SPAWNED + 4; spawned += 7) {
                            for (int near = 0; near <= GuardPlan.PER_EXIT_ALIVE; near += 2) {
                                int[] atExit = new int[exits];
                                for (int e = 0; e < exits; e++) atExit[e] = (e * 2 + wave) % (GuardPlan.PER_EXIT_ALIVE + 1);
                                int[] a = GuardPlan.assign(wave, exits, atExit, player, near, alive, spawned);
                                assertTrue(a.length <= GuardPlan.count(wave, exits, player, alive, spawned));
                                assertTrue(a.length == 0 || alive + a.length <= GuardPlan.MAX_ALIVE);
                                assertTrue(a.length == 0 || spawned + a.length <= GuardPlan.MAX_SPAWNED);
                                int[] load = atExit.clone();
                                int fromPlayer = 0;
                                for (int slot : a) {
                                    assertTrue(slot == GuardPlan.PLAYER || (slot >= 0 && slot < exits), "a real exit or the player: " + slot);
                                    if (slot == GuardPlan.PLAYER) {
                                        fromPlayer++;
                                        assertTrue(player, "no player, no share");
                                    } else {
                                        load[slot]++;
                                        assertTrue(load[slot] <= Math.max(GuardPlan.PER_EXIT_ALIVE, atExit[slot]), "an exit never past its ceiling");
                                    }
                                }
                                assertTrue(near + fromPlayer <= Math.max(GuardPlan.PER_EXIT_ALIVE, near), "a player never past the ceiling");
                                assertTrue(fromPlayer <= GuardPlan.playerShare(wave));
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void aWholeCollapseNeverPassesTheCeilingsWhateverTheExitsAndThePlayers() {
        for (int exits = 0; exits <= 8; exits++) {
            for (boolean player : new boolean[]{false, true}) {
                for (int deaths = 0; deaths <= 30; deaths += 6) {              // how many of the horde the players kill between two waves
                    int[] atExit = new int[exits];
                    int atPlayer = 0, alive = 0, spawned = 0;
                    for (int wave = 0; wave < GuardPlan.WAVES.length; wave++) {
                        int[] plan = GuardPlan.assign(wave, exits, atExit, player, atPlayer, alive, spawned);
                        for (int slot : plan) {
                            if (slot == GuardPlan.PLAYER) atPlayer++;
                            else atExit[slot]++;
                        }
                        alive += plan.length;
                        spawned += plan.length;
                        assertTrue(alive <= GuardPlan.MAX_ALIVE, "alive " + alive + " with " + exits + " exits, " + deaths + " killed");
                        assertTrue(spawned <= GuardPlan.MAX_SPAWNED, "made " + spawned);
                        for (int e : atExit) assertTrue(e <= GuardPlan.PER_EXIT_ALIVE);
                        assertTrue(atPlayer <= GuardPlan.PER_EXIT_ALIVE);
                        for (int k = 0; k < deaths && alive > 0; k++) {          // the busiest place loses one
                            int busiest = -1, most = atPlayer;
                            for (int e = 0; e < exits; e++) if (atExit[e] > most) {
                                most = atExit[e];
                                busiest = e;
                            }
                            if (most == 0) break;
                            if (busiest < 0) atPlayer--;
                            else atExit[busiest]--;
                            alive--;
                        }
                    }
                }
            }
        }
        // with everything killed between waves, three exits and a player would want 61 and are held to 56 in all
        int[] atExit = new int[3];
        int spawned = 0;
        for (int wave = 0; wave < GuardPlan.WAVES.length; wave++) {
            int[] plan = GuardPlan.assign(wave, 3, atExit, true, 0, 0, spawned);
            spawned += plan.length;
        }
        assertEquals(GuardPlan.MAX_SPAWNED, spawned, "MAX_SPAWNED is reached and not passed");
        int one = 0;
        for (int wave = 0; wave < GuardPlan.WAVES.length; wave++) one += GuardPlan.assign(wave, 1, new int[1], true, 0, 0, one).length;
        assertEquals(27, one, "one exit and a player: 3 + 3 + 5 + 5 + 6 + 5");
    }

    @Test
    void theHordeIsDeterministic() {
        for (int wave = 0; wave < GuardPlan.WAVES.length; wave++) {
            assertArrayEquals(GuardPlan.assign(wave, 5, new int[]{1, 0, 2, 0, 3}, true, 2, 9, 12), GuardPlan.assign(wave, 5, new int[]{1, 0, 2, 0, 3}, true, 2, 9, 12));
        }
    }

    // ---------------------------------------------------------------- who the player's share goes to
    @Test
    void thePlayerFollowedIsTheOneFarthestFromEveryExit() {
        double[] px = {0.5, 50.5, 10.5}, pz = {0.5, 0.5, 10.5};
        assertEquals(1, GuardPlan.targetPlayer(0, px, pz, new int[]{0}, new int[]{0}), "the one 50 blocks from the only exit");
        assertEquals(1, GuardPlan.targetPlayer(5, px, pz, new int[]{0}, new int[]{0}), "the wave does not matter when there are exits");
        double[] qx = {25.5, 0.5, 49.5}, qz = {0.5, 5.5, 0.5};
        assertEquals(0, GuardPlan.targetPlayer(0, qx, qz, new int[]{0, 50}, new int[]{0, 0}), "midway between two exits is farther from both than a player at either");
        assertEquals(0, GuardPlan.targetPlayer(0, new double[]{10, 10}, new double[]{10, 10}, new int[]{0}, new int[]{0}), "equal distances: the first");
        assertEquals(0, GuardPlan.targetPlayer(3, new double[]{7}, new double[]{7}, new int[]{1}, new int[]{1}), "a single player");
    }

    @Test
    void withNoExitsThePlayersAreTakenInTurnByWave() {
        double[] px = {0, 10, 20}, pz = {0, 0, 0};
        for (int wave = 0; wave < 6; wave++) assertEquals(wave % 3, GuardPlan.targetPlayer(wave, px, pz, new int[0], new int[0]));
        assertEquals(1, GuardPlan.targetPlayer(-2, px, pz, null, null), "a negative wave still turns");
    }

    @Test
    void nobodyToFollowIsMinusOneAndBrokenInputDoesNotThrow() {
        assertEquals(-1, GuardPlan.targetPlayer(0, new double[0], new double[0], new int[]{0}, new int[]{0}));
        assertEquals(-1, GuardPlan.targetPlayer(0, null, null, new int[]{0}, new int[]{0}));
        assertEquals(-1, GuardPlan.targetPlayer(0, new double[]{1}, null, new int[]{0}, new int[]{0}));
        assertEquals(0, GuardPlan.targetPlayer(0, new double[]{Double.NaN, Double.NaN}, new double[]{0, 0}, new int[]{0}, new int[]{0}), "NaN positions pick the first and do not throw");
        assertEquals(1, GuardPlan.targetPlayer(0, new double[]{1, 9, 3}, new double[]{0, 0, 0, 5}, new int[]{0, 5, 9}, new int[]{0}), "lists of unequal length use the shorter");
    }

    // ---------------------------------------------------------------- tier of the fallback roster
    @Test
    void theTierFollowsTheRollAndTheWave() {
        assertEquals(2, GuardPlan.tier(0, 0), "an early wave is tier 2 whatever the roll");
        assertEquals(2, GuardPlan.tier(0, 99));
        assertEquals(3, GuardPlan.tier(5, 10), "a late wave is half tier 3");
        assertEquals(2, GuardPlan.tier(5, 80));
        assertEquals(2, GuardPlan.tier(-1, 0));
        assertEquals(2, GuardPlan.tier(40, 0));
        int tier3 = 0;
        for (int roll = 0; roll < 100; roll++) if (GuardPlan.tier(3, roll) == 3) tier3++;
        assertEquals(GuardPlan.WAVES[3].tier3Percent(), tier3);
    }

    // ---------------------------------------------------------------- the frenzy
    @Test
    void theFrenzyIsThirtyPercentFasterAndAQuarterHarderAndFinite() {
        assertEquals(0.30, GuardPlan.FRENZY_SPEED, 1e-12, "speed +30%");
        assertEquals(0.25, GuardPlan.FRENZY_ATTACK, 1e-12, "attack damage +25%");
        assertEquals(0.0, GuardPlan.extra(40.0, GuardPlan.FRENZY_ATTACK, GuardPlan.FRENZY_ATTACK_CAP), 1e-9, "a quarter of 40 would be 10, but the total is held to 40: nothing is added");
    }

    @Test
    void theAttackGrowsByAQuarterButNeverPastTheCapOrBelowTheBase() {
        assertEquals(2.5, GuardPlan.extra(10.0, GuardPlan.FRENZY_ATTACK, GuardPlan.FRENZY_ATTACK_CAP), 1e-9);
        assertEquals(6.0, GuardPlan.extra(24.0, GuardPlan.FRENZY_ATTACK, GuardPlan.FRENZY_ATTACK_CAP), 1e-9);
        assertEquals(0.0, GuardPlan.extra(GuardPlan.FRENZY_ATTACK_CAP, GuardPlan.FRENZY_ATTACK, GuardPlan.FRENZY_ATTACK_CAP), "already at the cap: nothing is added");
        assertEquals(0.0, GuardPlan.extra(GuardPlan.FRENZY_ATTACK_CAP * 3, GuardPlan.FRENZY_ATTACK, GuardPlan.FRENZY_ATTACK_CAP), "past it: nothing, the creature is never weakened");
        assertEquals(4.0, GuardPlan.extra(36.0, GuardPlan.FRENZY_ATTACK, GuardPlan.FRENZY_ATTACK_CAP), 1e-9, "held to the cap");
        for (double base = 0.5; base < 200; base *= 1.3) {
            double add = GuardPlan.extra(base, GuardPlan.FRENZY_ATTACK, GuardPlan.FRENZY_ATTACK_CAP);
            assertTrue(add >= 0.0 && (add == 0.0 || base + add <= GuardPlan.FRENZY_ATTACK_CAP + 1e-9));
            assertTrue(add <= base * GuardPlan.FRENZY_ATTACK + 1e-9, "never more than a quarter");
        }
        assertTrue(GuardPlan.FRENZY_SIGHT >= 32.0 && GuardPlan.FRENZY_SIGHT <= 128.0, "a long sight, not an endless one");
    }

    @Test
    void theOldMutationIsKeptAndStillFiniteAndNeverWeakensAnythingStronger() {
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

    // ---------------------------------------------------------------- the ring, when no wall can birth one
    @Test
    void aCreatureStandsOnTheRingAroundItsSlotAndNotTwiceInARow() {
        for (int slot = GuardPlan.PLAYER; slot < 8; slot++) {
            for (int n = 0; n < 10; n++) {
                int[] last = null;
                for (int attempt = 0; attempt < 6; attempt++) {
                    int[] o = GuardPlan.offset(slot, n, attempt);
                    int d = Math.max(Math.abs(o[0]), Math.abs(o[1]));
                    assertTrue(d >= GuardPlan.RING_MIN && d <= GuardPlan.RING_MAX, "on the ring: " + d);
                    assertArrayEquals(o, GuardPlan.offset(slot, n, attempt), "stable");
                    if (last != null) assertFalse(last[0] == o[0] && last[1] == o[1], "a new try is a new spot");
                    last = o;
                }
            }
        }
        assertTrue(GuardPlan.RING_MIN + GuardPlan.PLAYER_RING > GuardPlan.MIN_PLAYER_GAP, "the player's ring, pushed out, clears the gap");
        int[] neg = GuardPlan.offset(-3, -5, -2);
        assertTrue(Math.max(Math.abs(neg[0]), Math.abs(neg[1])) >= GuardPlan.RING_MIN, "negative numbers are fine");
    }
}
