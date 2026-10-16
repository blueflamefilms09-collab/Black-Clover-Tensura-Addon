package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.avatar.dnd.AvatarStats;
import com.newuniverse.nusmp.avatar.dnd.Stat;
import com.newuniverse.nusmp.avatar.dnd.StatRoller;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the creation roll must be the tabletop one (4d6 drop lowest, 5d6 keep the best three for a lucky creator), seedable, and bounded. */
class StatRollerTest {
    @Test
    void fourDiceDropTheLowestKeepsAtLeastThreeAndAtMostEighteen() {
        Random r = new Random(7);
        for (int i = 0; i < 5000; i++) {
            int v = StatRoller.rollOne(r, 4, 3);
            assertTrue(v >= 3 && v <= 18, "score " + v);
        }
    }

    @Test
    void theSameSeedGivesTheSameRoll() {
        assertArrayEquals(StatRoller.roll(new Random(42), false), StatRoller.roll(new Random(42), false));
        assertArrayEquals(StatRoller.roll(new Random(42), true), StatRoller.roll(new Random(42), true));
    }

    @Test
    void sixScoresAreRolled() { assertEquals(6, StatRoller.roll(new Random(1), false).length); }

    @Test
    void dropLowestReallyDropsTheLowest() {
        // a fake Random that yields the dice 1,6,6,6 (nextInt(6) = 0,5,5,5): the kept sum must be 18, not 19
        Random fixed = new Random() {
            private final int[] seq = {0, 5, 5, 5};
            private int i;
            @Override public int nextInt(int bound) { return seq[i++ % seq.length]; }
        };
        assertEquals(18, StatRoller.rollOne(fixed, 4, 3));
    }

    @Test
    void fiveDiceKeepingThreeBeatsFourDiceOnAverage() {
        double four = 0, five = 0;
        int n = 20000;
        Random a = new Random(11), b = new Random(11);
        for (int i = 0; i < n; i++) { four += StatRoller.rollOne(a, 4, 3); five += StatRoller.rollOne(b, 5, 3); }
        assertTrue(five / n > four / n + 0.3, "5d6 drop 2 must be clearly better: " + five / n + " vs " + four / n);
    }

    @Test
    void theLuckThresholdIsStrict() {
        assertFalse(StatRoller.usesFiveDice(10.0, 10));
        assertTrue(StatRoller.usesFiveDice(10.01, 10));
        assertFalse(StatRoller.usesFiveDice(-5, 10));
    }

    @Test
    void modifierMatchesDnd() {
        assertEquals(0, Stat.modifier(10));
        assertEquals(0, Stat.modifier(11));
        assertEquals(1, Stat.modifier(12));
        assertEquals(-1, Stat.modifier(9));
        assertEquals(-5, Stat.modifier(1));
        assertEquals(4, Stat.modifier(18));
    }

    @Test
    void derivedStatsGrowWithScoresAndRaceMultipliers() {
        int[] low = {8, 8, 8, 8, 8, 8}, high = {16, 16, 16, 16, 16, 16};
        AvatarStats.Derived a = AvatarStats.derive(low, Map.of(), 1000, 1.0), b = AvatarStats.derive(high, Map.of(), 1000, 1.0);
        assertTrue(b.maxHealth() > a.maxHealth());
        assertTrue(b.attackDamage() > a.attackDamage());
        assertTrue(b.ep() > a.ep());
        AvatarStats.Derived strong = AvatarStats.derive(high, Map.of(Stat.STR, 1.5), 1000, 1.0);
        assertTrue(strong.attackDamage() > b.attackDamage());
        AvatarStats.Derived surged = AvatarStats.derive(high, Map.of(), 1000, 2.0);
        assertEquals(b.ep() * 2.0, surged.ep(), 1e-6);
    }

    @Test
    void derivedStatsAreNeverBelowTheirFloors() {
        AvatarStats.Derived d = AvatarStats.derive(new int[]{1, 1, 1, 1, 1, 1}, Map.of(), 1, 1.0);
        assertTrue(d.maxHealth() >= 10 && d.attackDamage() >= 1 && d.moveSpeed() >= 0.18 && d.armor() >= 0 && d.ep() >= 1 && d.magicule() >= 10 && d.aura() >= 10);
    }
}
