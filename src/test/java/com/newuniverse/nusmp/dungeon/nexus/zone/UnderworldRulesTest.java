package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.blackclover.Devil;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The numbers of the Underworld Gate's creatures: Disciples, their shares of a Devil's power, where they appear. */
class UnderworldRulesTest {
    @Test
    void theCapGrowsWithThePlayersAndStopsAtTwelve() {
        assertEquals(0, UnderworldRules.discipleCap(0));
        assertEquals(6, UnderworldRules.discipleCap(1));
        assertEquals(8, UnderworldRules.discipleCap(2));
        assertEquals(10, UnderworldRules.discipleCap(3));
        assertEquals(12, UnderworldRules.discipleCap(4));
        assertEquals(12, UnderworldRules.discipleCap(40));
        for (int n = 1; n < 30; n++) assertTrue(UnderworldRules.discipleCap(n + 1) >= UnderworldRules.discipleCap(n));
    }

    @Test
    void aHallStartsWithSomeDisciplesButNeverMoreThanItsCap() {
        for (int n = 0; n < 8; n++) {
            int start = UnderworldRules.initialDisciples(n);
            assertTrue(start >= 3);
            assertTrue(start <= UnderworldRules.CAP_MAX);
        }
    }

    @Test
    void theyComeBackSlowlyAndNotAtAllAtTheCap() {
        assertEquals(-1, UnderworldRules.respawnDelay(6, 6));
        assertEquals(-1, UnderworldRules.respawnDelay(9, 6));
        assertEquals(-1, UnderworldRules.respawnDelay(0, 0));
        int last = 0;
        for (int alive = 5; alive >= 0; alive--) {
            int delay = UnderworldRules.respawnDelay(alive, 6);
            assertTrue(delay >= UnderworldRules.RESPAWN_FAST && delay <= UnderworldRules.RESPAWN_SLOW, "delay " + delay);
            if (alive < 5) assertTrue(delay <= last, "an emptier ruin refills a little faster");
            last = delay;
        }
        assertEquals(UnderworldRules.RESPAWN_FAST, UnderworldRules.respawnDelay(0, 6));
        assertTrue(UnderworldRules.respawnDelay(5, 6) > 1000);
    }

    @Test
    void theSharesRiseFromATenthToTwoFifthsInFives() {
        assertEquals(0.10f, UnderworldRules.fraction(0.0), 1e-6f);
        assertEquals(0.40f, UnderworldRules.fraction(1.0), 1e-6f);
        assertEquals(0.10f, UnderworldRules.fraction(-3.0), 1e-6f);
        assertEquals(0.40f, UnderworldRules.fraction(9.0), 1e-6f);
        float last = 0;
        Set<Integer> seen = new java.util.TreeSet<>();
        for (int i = 0; i <= 100; i++) {
            float f = UnderworldRules.fraction(i / 100.0);
            assertTrue(f >= last - 1e-6f, "never falls as the depth rises");
            last = f;
            assertEquals(0, UnderworldRules.percent(f) % 5, "whole fives of a percent");
            seen.add(UnderworldRules.percent(f));
        }
        assertEquals(Set.of(10, 15, 20, 25, 30, 35, 40), seen);
    }

    @Test
    void theThreeFamiliesChannelTheirOwnDevils() {
        Set<Devil> seen = EnumSet.noneOf(Devil.class);
        for (int n = 0; n < 9; n++) seen.add(UnderworldRules.channel(n));
        assertEquals(EnumSet.of(Devil.LUCIFERO, Devil.MEGICULA, Devil.BEELZEBUB), seen);
        assertEquals(UnderworldRules.channel(1), UnderworldRules.channel(-2), "negative counters wrap");
        assertEquals(Devil.LUCIFERO, UnderworldRules.channel(0));
    }

    @Test
    void aDiscipleNeverAppearsNearAPlayer() {
        double[][] spots = {{0, 0}, {30, 0}, {60, 0}, {0, 70}};
        double[][] players = {{2, 1}};
        for (long salt = 0; salt < 20; salt++) {
            int i = UnderworldRules.pickSpot(spots, players, salt);
            assertTrue(i >= 1, "spot 0 is next to the player");
        }
        assertEquals(-1, UnderworldRules.pickSpot(new double[][]{{1, 1}, {5, 5}}, players, 3));
        assertEquals(-1, UnderworldRules.pickSpot(new double[0][], players, 3));
        assertTrue(UnderworldRules.pickSpot(spots, new double[0][], 4) >= 0, "with nobody about every spot is free");
        Set<Integer> picked = new java.util.HashSet<>();
        for (long salt = 0; salt < 50; salt++) picked.add(UnderworldRules.pickSpot(spots, players, salt));
        assertEquals(3, picked.size(), "the salt moves the choice among the free spots");
        assertEquals(UnderworldRules.pickSpot(spots, players, -5), UnderworldRules.pickSpot(spots, players, -5), "and a negative salt is fine");
    }

    @Test
    void theGatekeeperStaysAtItsPostUnlessItIsFighting() {
        assertFalse(UnderworldRules.shouldReturn(3.0, false));
        assertFalse(UnderworldRules.shouldReturn(40.0, true), "it chases what it fights");
        assertTrue(UnderworldRules.shouldReturn(UnderworldRules.KEEPER_LEASH + 1, false));
    }

    @Test
    void aDevilOfLesserRankIsFedBeforeItStarvesOrSleeps() {
        assertTrue(UnderworldRules.KEEP_LOW > 40, "above the 'hungry' mark of the Dungeon's food web, so it never hunts");
        assertTrue(UnderworldRules.KEEP_FULL < 80, "under the 'full' mark, so it never falls asleep");
        assertTrue(UnderworldRules.needsFeeding(UnderworldRules.KEEP_LOW - 1));
        assertFalse(UnderworldRules.needsFeeding(UnderworldRules.KEEP_LOW));
    }

    @Test
    void theHallPlacesItsOwnDevilsAndDisciplesOnlyWhileTheSentientDungeonIsOff() {
        assertFalse(UnderworldRules.staticPopulation(true), "the Sentient Dungeon brings Type C's low-tier Devils and Disciples out of its walls");
        assertTrue(UnderworldRules.staticPopulation(false), "with it off the hall is the hall of 1.21.0");
    }

    @Test
    void aHallIsGuardedByTheSameAvatarForeverAndBothAreMetAcrossTheCore() {
        int lucifero = 0, megicula = 0;
        for (int cx = -12; cx <= 12; cx++) {
            for (int cz = -12; cz <= 12; cz++) {
                int v = UnderworldRules.avatarVariant(cx, cz);
                assertTrue(v == 0 || v == 1, "0 the Avatar of Lucifero, 1 the Avatar of Megicula");
                assertEquals(v, UnderworldRules.avatarVariant(cx, cz), "a hall always has the same Avatar");
                if (v == 0) lucifero++;
                else megicula++;
            }
        }
        assertTrue(lucifero > 150 && megicula > 150, "both Avatars are met across the Variable Core: " + lucifero + " and " + megicula);
    }

    @Test
    void theNamesAreWritten() {
        assertEquals("Dark Disciple of Lucifero (25%)", UnderworldText.discipleName("Lucifero", 25));
        assertFalse(UnderworldText.GATEKEEPER.isEmpty());
        assertFalse(UnderworldText.ENTER.isEmpty());
    }
}
