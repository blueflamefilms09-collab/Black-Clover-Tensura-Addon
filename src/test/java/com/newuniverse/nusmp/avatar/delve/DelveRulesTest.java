package com.newuniverse.nusmp.avatar.delve;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The limits of a delve: when it may start, when it must end, how deep it goes and the order in which it explores. */
class DelveRulesTest {
    private static DelveRules.Status ok() { return new DelveRules.Status(1.0, 0, false, false, false, true); }

    @Test
    void aHealthyTripTurnsBackForNothing() {
        assertEquals(DelveRules.Reason.NONE, DelveRules.mustReturn(ok(), 0.35, 12_000));
    }

    @Test
    void everyReasonToTurnBackIsHonoured() {
        assertEquals(DelveRules.Reason.HURT, DelveRules.mustReturn(new DelveRules.Status(0.3, 0, false, false, false, true), 0.35, 12_000));
        assertEquals(DelveRules.Reason.TIME, DelveRules.mustReturn(new DelveRules.Status(1.0, 12_000, false, false, false, true), 0.35, 12_000));
        assertEquals(DelveRules.Reason.POCKETS_FULL, DelveRules.mustReturn(new DelveRules.Status(1.0, 0, true, false, false, true), 0.35, 12_000));
        assertEquals(DelveRules.Reason.TOLD, DelveRules.mustReturn(new DelveRules.Status(1.0, 0, false, true, false, true), 0.35, 12_000));
        assertEquals(DelveRules.Reason.CREATOR_GONE, DelveRules.mustReturn(new DelveRules.Status(1.0, 0, false, false, true, true), 0.35, 12_000));
        assertEquals(DelveRules.Reason.DISABLED, DelveRules.mustReturn(new DelveRules.Status(1.0, 0, false, false, false, false), 0.35, 12_000));
    }

    @Test
    void theMostUrgentReasonWins() {
        DelveRules.Status all = new DelveRules.Status(0.1, 99_999, true, true, true, false);
        assertEquals(DelveRules.Reason.DISABLED, DelveRules.mustReturn(all, 0.35, 12_000));
        assertEquals(DelveRules.Reason.CREATOR_GONE, DelveRules.mustReturn(new DelveRules.Status(0.1, 99_999, true, true, true, true), 0.35, 12_000));
        assertEquals(DelveRules.Reason.TOLD, DelveRules.mustReturn(new DelveRules.Status(0.1, 99_999, true, true, false, true), 0.35, 12_000));
        assertEquals(DelveRules.Reason.HURT, DelveRules.mustReturn(new DelveRules.Status(0.1, 99_999, true, false, false, true), 0.35, 12_000));
    }

    @Test
    void theTimeBudgetHasAFloorOfOneMinute() {
        assertEquals(12_000, DelveRules.budgetTicks(10));
        assertEquals(1200, DelveRules.budgetTicks(0));
        assertEquals(1200, DelveRules.budgetTicks(-3));
    }

    @Test
    void aTripStartsOnlyWhenEverythingAllowsIt() {
        assertTrue(DelveRules.mayStart(true, true, true, true, 0.9, 0.7, 5_000, 4_000));
        assertFalse(DelveRules.mayStart(false, true, true, true, 0.9, 0.7, 5_000, 4_000), "switch off");
        assertFalse(DelveRules.mayStart(true, false, true, true, 0.9, 0.7, 5_000, 4_000), "no dungeon");
        assertFalse(DelveRules.mayStart(true, true, false, true, 0.9, 0.7, 5_000, 4_000), "not told to delve");
        assertFalse(DelveRules.mayStart(true, true, true, false, 0.9, 0.7, 5_000, 4_000), "betrayed or independent");
        assertFalse(DelveRules.mayStart(true, true, true, true, 0.5, 0.7, 5_000, 4_000), "wounded");
        assertFalse(DelveRules.mayStart(true, true, true, true, 0.9, 0.7, 3_000, 4_000), "resting after the last trip");
    }

    @Test
    void theLandingIsNeverDeeperThanTheCreatorTheDungeonOrTheCap() {
        assertEquals(0, DelveRules.landingIndex(0, 5, 4));
        assertEquals(3, DelveRules.landingIndex(3, 5, 4));
        assertEquals(4, DelveRules.landingIndex(9, 5, 4), "the newest finished sector");
        assertEquals(2, DelveRules.landingIndex(9, 5, 2), "the configured cap");
        assertEquals(0, DelveRules.landingIndex(-3, 5, 4));
        assertEquals(-1, DelveRules.landingIndex(3, 0, 4), "no playable sector");
        assertEquals(0, DelveRules.landingIndex(3, 5, -1));
    }

    @Test
    void theWalkVisitsTheNearestUnvisitedRoomFirst() {
        // 0 - 1 - 2 - 3, and 1 - 4
        Map<Integer, List<Integer>> g = new HashMap<>();
        g.put(0, List.of(1)); g.put(1, List.of(0, 2, 4)); g.put(2, List.of(1, 3)); g.put(3, List.of(2)); g.put(4, List.of(1));
        Set<Integer> visited = new HashSet<>(Set.of(0));
        assertEquals(1, DelveRules.nextRoom(g, 0, visited));
        visited.add(1);
        assertEquals(2, DelveRules.nextRoom(g, 1, visited), "ties go to the lower id");
        visited.add(2);
        assertEquals(3, DelveRules.nextRoom(g, 2, visited), "3 is one doorway from 2, 4 is two");
        visited.add(3);
        assertEquals(4, DelveRules.nextRoom(g, 3, visited), "back through 2 and 1 to the side room");
        visited.add(4);
        assertEquals(-1, DelveRules.nextRoom(g, 4, visited));
    }

    @Test
    void theWalkIsRepeatableAndIgnoresUnreachableRooms() {
        Map<Integer, List<Integer>> g = new HashMap<>();
        g.put(0, List.of(1)); g.put(1, List.of(0)); g.put(7, List.of(8)); g.put(8, List.of(7));
        Set<Integer> visited = new HashSet<>(Set.of(0, 1));
        assertEquals(-1, DelveRules.nextRoom(g, 0, visited), "7 and 8 are on the other side of a wall");
        assertEquals(-1, DelveRules.nextRoom(Map.of(), 5, Set.of()), "an empty floor");
    }

    @Test
    void stuckMeansHardlyMovingForAWhile() {
        assertTrue(DelveRules.stuck(0.4, 100));
        assertFalse(DelveRules.stuck(0.4, 99));
        assertFalse(DelveRules.stuck(8.0, 400));
    }

    @Test
    void aStrongerFoeSendsTheAvatarHomeButNotBeforeTheWoundsOrTheCall() {
        assertEquals(DelveRules.Reason.OUTMATCHED, DelveRules.mustReturn(ok(), 0.35, 12_000, true));
        assertEquals(DelveRules.Reason.NONE, DelveRules.mustReturn(ok(), 0.35, 12_000, false));
        assertEquals(DelveRules.Reason.HURT, DelveRules.mustReturn(new DelveRules.Status(0.1, 0, false, false, false, true), 0.35, 12_000, true), "wounds first");
        assertEquals(DelveRules.Reason.TOLD, DelveRules.mustReturn(new DelveRules.Status(1.0, 0, false, true, false, true), 0.35, 12_000, true), "the call first");
        assertEquals(DelveRules.Reason.OUTMATCHED, DelveRules.mustReturn(new DelveRules.Status(1.0, 99_999, true, false, false, true), 0.35, 12_000, true), "before the load and the clock");
    }

    @Test
    void aBossIsFoughtOnlyWhenItIsNoStrongerThanTheAvatar() {
        assertTrue(DelveRules.worthEngaging(1000, 900, true));
        assertTrue(DelveRules.worthEngaging(1000, 1000, true));
        assertFalse(DelveRules.worthEngaging(1000, 1001, true));
        assertTrue(DelveRules.worthEngaging(1000, 3000, false), "an ordinary monster up to three times the Avatar");
        assertFalse(DelveRules.worthEngaging(1000, 3001, false));
        assertTrue(DelveRules.worthEngaging(0, 500, false), "unknown strength of the Avatar: ordinary monsters are fought");
        assertFalse(DelveRules.worthEngaging(0, 500, true), "but never a boss");
        assertTrue(DelveRules.worthEngaging(1000, 0, false), "a monster without EP is fought");
        assertFalse(DelveRules.worthEngaging(1000, 0, true));
    }

    @Test
    void everyReasonHasAWordForTheCreator() {
        for (DelveRules.Reason r : DelveRules.Reason.values()) {
            String text = DelveRules.describe(r);
            assertNotNull(text);
            assertFalse(text.isBlank(), r.name());
        }
        assertNotEquals(DelveRules.describe(DelveRules.Reason.HURT), DelveRules.describe(DelveRules.Reason.TIME));
    }
}
