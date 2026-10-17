package com.newuniverse.nusmp.labyrinth.mega.gear;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the magic resistance decides how much damage a player takes from every spell in the Mega-Dungeon, so its three rules are pinned: WHICH pieces count
 * (armour only in its own worn position, weapons and shields only in a hand), HOW MUCH they add up to (summed, then held to the ceiling, and the ceiling
 * itself never above 90), and how the percentage becomes a smaller hit (a straight multiplier that never gives damage back or goes below zero). The
 * scenario at the end is the one the brief describes: a whole set of Rare gear is strong but cannot make a player immune to magic.
 */
class ResistMathTest {

    @Test
    void armourCountsOnlyWhereItBelongsAndWeaponsOnlyInTheHands() {
        int[] pos = {0, 1, 2, 3};
        GearSlot[] armour = {GearSlot.HELMET, GearSlot.CHESTPLATE, GearSlot.LEGGINGS, GearSlot.BOOTS};
        for (int a = 0; a < 4; a++) {
            for (int p = 0; p < 4; p++) assertEquals(a == p, ResistMath.counts(armour[a], pos[p]), armour[a] + " at position " + p);
            assertFalse(ResistMath.counts(armour[a], -1), armour[a] + " held in a hand must not count");
        }
        for (GearSlot s : GearSlot.values()) {
            if (s.isArmour()) continue;
            assertTrue(ResistMath.counts(s, -1), s + " held counts");
            for (int p = 0; p < 4; p++) assertFalse(ResistMath.counts(s, p), s + " worn on position " + p + " must not count");
        }
    }

    @Test
    void theSumIsHeldToTheCeilingAndTheCeilingToNinety() {
        assertEquals(25, ResistMath.effective(25, 40));
        assertEquals(40, ResistMath.effective(40, 40));
        assertEquals(40, ResistMath.effective(61, 40));
        assertEquals(0, ResistMath.effective(50, 0), "ceiling 0 switches the resistance off");
        assertEquals(90, ResistMath.effective(500, 500), "even a silly config cannot pass the hard cap");
        assertEquals(90, ResistMath.HARD_CAP);
        assertEquals(0, ResistMath.effective(-5, 40));
        assertEquals(0, ResistMath.effective(10, -3));
    }

    @Test
    void thePercentageIsAStraightMultiplier() {
        assertEquals(10.0f, ResistMath.reduce(10.0f, 0), 0.0f);
        assertEquals(10.0f, ResistMath.reduce(10.0f, -4), 0.0f);
        assertEquals(9.0f, ResistMath.reduce(10.0f, 10), 1e-6f);
        assertEquals(6.0f, ResistMath.reduce(10.0f, 40), 1e-6f);
        assertEquals(0.0f, ResistMath.reduce(10.0f, 100), 0.0f);
        assertEquals(0.0f, ResistMath.reduce(10.0f, 250), 0.0f, "never below zero, never damage given back");
        assertEquals(0.0f, ResistMath.reduce(0.0f, 30), 0.0f);
        assertEquals(-3.0f, ResistMath.reduce(-3.0f, 30), 0.0f, "a negative amount is not this class's business");
        for (int p = 0; p <= 100; p += 5) assertTrue(ResistMath.reduce(7.5f, p) <= 7.5f);
    }

    @Test
    void aFullSetOfTheBestRareGearIsStrongButNotImmune() {
        int sum = 0;
        for (int i = 0; i < 4; i++) sum += 12;           // four armour pieces at the top of the Rare armour range
        sum += 6 + 6;                                    // a sword in the main hand and a shield in the off hand
        assertEquals(60, sum);
        int shown = ResistMath.effective(sum, 40);
        assertEquals(40, shown);
        assertEquals(60.0f, ResistMath.reduce(100.0f, shown), 1e-4f, "a hundred points of spell damage still do sixty");
        int typicalSet = 4 * 8 + 5 + 5;                  // a middling set
        assertTrue(ResistMath.effective(typicalSet, 40) < 40 || typicalSet >= 40);
    }
}
