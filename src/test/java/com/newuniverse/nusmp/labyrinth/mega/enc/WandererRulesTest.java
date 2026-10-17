package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a wandering boss is only a game if the players can win by being clever, and the rules of what it perceives are the rules of that game; these tests pin
 * the sight modifiers, the walls and the safe rooms that hide, the close range nobody hides from, the hearing, and the chance that a boss appears.
 */
class WandererRulesTest {
    private static final WandererRules.Tuning T = WandererRules.of(24, 40, 160);
    private static final UUID ID = new UUID(0, 1);

    private static WandererRules.Sighting seen(double distance, boolean los, boolean sneaking, boolean dark, boolean still, boolean safe) {
        return new WandererRules.Sighting(ID, distance, 65, 0, distance, los, sneaking, dark, still, safe);
    }

    @Test
    void theSightRangeShrinksWhenCrouchingInTheDarkAndStandingStill() {
        assertEquals(24.0, WandererRules.effectiveSight(T, false, false, false), 1e-9);
        assertEquals(12.0, WandererRules.effectiveSight(T, true, false, false), 1e-9);
        assertEquals(14.4, WandererRules.effectiveSight(T, false, true, false), 1e-9);
        assertEquals(19.2, WandererRules.effectiveSight(T, false, false, true), 1e-9);
        assertEquals(24 * 0.5 * 0.6 * 0.8, WandererRules.effectiveSight(T, true, true, true), 1e-9);
    }

    @Test
    void aLitMovingPlayerIsSeenAcrossTheHallButACrouchingOneInTheDarkIsNot() {
        assertTrue(WandererRules.notices(T, seen(23, true, false, false, false, false)));
        assertFalse(WandererRules.notices(T, seen(25, true, false, false, false, false)));
        assertFalse(WandererRules.notices(T, seen(13, true, true, false, false, false)), "crouching halves the range");
        assertTrue(WandererRules.notices(T, seen(11, true, true, false, false, false)));
        assertFalse(WandererRules.notices(T, seen(8, true, true, true, true, false)), "crouching, in the dark, standing still: under six blocks");
        assertTrue(WandererRules.notices(T, seen(5, true, true, true, true, false)));
    }

    @Test
    void noLineOfSightMeansNotSeenAtAnyDistance() {
        assertFalse(WandererRules.notices(T, seen(1, false, false, false, false, false)));
        assertFalse(WandererRules.notices(T, seen(10, false, false, false, false, false)));
    }

    @Test
    void nobodyInASafeRoomIsEverNoticed() {
        assertFalse(WandererRules.notices(T, seen(1, true, false, false, false, true)));
        assertFalse(WandererRules.notices(T, seen(10, true, false, false, false, true)));
    }

    @Test
    void nobodyHidesFromSomethingStandingNextToThem() {
        WandererRules.Tuning shortSighted = WandererRules.of(4, 40, 160);       // 4 x 0.5 x 0.6 x 0.8 = about one block of sight
        assertTrue(WandererRules.notices(shortSighted, seen(3.0, true, true, true, true, false)));
        assertFalse(WandererRules.notices(shortSighted, seen(3.1, true, true, true, true, false)));
        assertFalse(WandererRules.notices(shortSighted, seen(2.0, false, true, true, true, false)), "but only with a line of sight");
    }

    @Test
    void theTuningIsClampedToSaneValues() {
        WandererRules.Tuning t = WandererRules.of(-5, -5, -5);
        assertTrue(t.sightRange() >= 4.0 && t.hearRange() >= 8.0 && t.loseTicks() >= 20);
    }

    @Test
    void loudSoundsCarryFartherAndQuietOnesDoNot() {
        assertEquals(40.0, WandererRules.hearingRadius(T, 6.0), 1e-9);
        assertEquals(40.0, WandererRules.hearingRadius(T, 20.0), 1e-9);
        assertEquals(20.0, WandererRules.hearingRadius(T, 3.0), 1e-9);
        assertEquals(10.0, WandererRules.hearingRadius(T, 1.0), 1e-9, "a quarter of the range at the least");
        assertTrue(WandererRules.hears(T, 6.0, 39));
        assertFalse(WandererRules.hears(T, 6.0, 41));
        assertTrue(WandererRules.hears(T, 1.0, 10));
        assertFalse(WandererRules.hears(T, 1.0, 11));
        assertFalse(WandererRules.hears(T, 0.5, 1), "a footstep is below the threshold whatever the distance");
        assertTrue(WandererRules.hears(T, 1.0, 0));
    }

    @Test
    void aLouderCloserSoundScoresHigher() {
        assertTrue(WandererRules.score(6, 10) > WandererRules.score(2, 10));
        assertTrue(WandererRules.score(4, 5) > WandererRules.score(4, 30));
        assertTrue(WandererRules.score(1, 0) > 0);
    }

    @Test
    void noWandererAppearsDuringTheMinimumGap() {
        assertEquals(0.0, WandererRules.spawnChance(0.1, 1, 0, 6000), 0.0);
        assertEquals(0.0, WandererRules.spawnChance(0.1, 20, 5999, 6000), 0.0);
        assertEquals(0.0, WandererRules.spawnChance(0.0, 5, 99999, 6000), 0.0);
        assertEquals(0.0, WandererRules.spawnChance(-1.0, 5, 99999, 6000), 0.0);
    }

    @Test
    void theChanceStartsAtTheBaseAndRisesToDoubleOverTwoMoreGaps() {
        assertEquals(0.1, WandererRules.spawnChance(0.1, 1, 6000, 6000), 1e-9);
        assertEquals(0.15, WandererRules.spawnChance(0.1, 1, 6000 + 6000, 6000), 1e-9);
        assertEquals(0.2, WandererRules.spawnChance(0.1, 1, 6000 + 12000, 6000), 1e-9);
        assertEquals(0.2, WandererRules.spawnChance(0.1, 1, 6000 + 120000, 6000), 1e-9, "the ramp stops at double");
    }

    @Test
    void deeperSectorsSeeWanderersMoreOftenWithinLimits() {
        double base = WandererRules.spawnChance(0.1, 1, 6000, 6000), deep = WandererRules.spawnChance(0.1, 11, 6000, 6000);
        assertEquals(0.1 * 1.8, deep, 1e-9);
        assertTrue(deep > base);
        assertEquals(0.1 * 2.5, WandererRules.spawnChance(0.1, 1000, 6000, 6000), 1e-9, "at most two and a half times");
        assertEquals(0.5, WandererRules.spawnChance(0.9, 1000, 100000, 6000), 1e-9, "never above one half");
        assertTrue(WandererRules.spawnChance(0.1, 0, 6000, 6000) == base, "threat below one is read as one");
        assertEquals(0.2, WandererRules.spawnChance(0.1, 1, 0, 0), 1e-9, "no gap: the full ramp at once");
    }
}
