package com.newuniverse.nusmp.labyrinth.mega.rift;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the decisions behind the 1.94.0 fixes of the rifts, pinned without a game: the box a player has to be in to be taken (it must stay the box the block entity always used), what a shortcut does while its
 * floor is being built (wait, then settle for a built floor), which pocket slot is given out, why the spawner says no, where a player is put down when they leave a pocket (OUTSIDE the tear they went in by), which
 * clicks count as a press on a plate, and when an empty pocket collapses.
 */
class RiftFixTest {
    @Test
    void theTearIsTheBlockWidenedByAPlayerAndTallerByOneAndSevenTenths() {
        // the old box: the block, 0.45 wider each side, 1.7 taller; a player's box is 0.6 wide and 1.8 tall
        assertTrue(RiftRules.inTear(10, 64, 10, 10.5, 64, 10.5), "standing in the block");
        assertTrue(RiftRules.inTear(10, 64, 10, 11.7, 64, 10.5), "the player's box overlaps the widened box by a hair");
        assertFalse(RiftRules.inTear(10, 64, 10, 11.8, 64, 10.5), "just out of reach");
        assertFalse(RiftRules.inTear(10, 64, 10, 9.2, 64, 10.5));
        assertTrue(RiftRules.inTear(10, 64, 10, 10.5, 66.6, 10.5), "the head of the tear");
        assertFalse(RiftRules.inTear(10, 64, 10, 10.5, 66.7, 10.5), "feet above the tear");
        assertTrue(RiftRules.inTear(10, 64, 10, 10.5, 62.3, 10.5), "head reaches into the block from below");
        assertFalse(RiftRules.inTear(10, 64, 10, 10.5, 62.1, 10.5), "a player under the block");
    }

    @Test
    void aShortcutWaitsThenSettlesAndNeverSettlesForNothing() {
        assertEquals(RiftRules.Forming.READY, RiftRules.forming(true, 0, -1));
        assertEquals(RiftRules.Forming.READY, RiftRules.forming(true, 99999, 7), "a built target is always taken");
        assertEquals(RiftRules.Forming.WAIT, RiftRules.forming(false, 0, 7));
        assertEquals(RiftRules.Forming.WAIT, RiftRules.forming(false, RiftRules.FORM_PATIENCE_TICKS - 1, 7));
        assertEquals(RiftRules.Forming.SETTLE, RiftRules.forming(false, RiftRules.FORM_PATIENCE_TICKS, 7));
        assertEquals(RiftRules.Forming.WAIT, RiftRules.forming(false, 99999, -1), "nothing built between the rift's floor and its target: keep waiting, there is nothing to settle for");
    }

    @Test
    void settlingPicksTheFarthestBuiltFloorBetweenTheRiftAndItsTarget() {
        Set<Integer> built = Set.of(4, 5, 6);
        assertEquals(6, RiftRules.settleTarget(3, 9, built::contains));
        assertEquals(5, RiftRules.settleTarget(3, 5, built::contains));
        assertEquals(-1, RiftRules.settleTarget(6, 9, built::contains), "the rift's own floor does not count");
        assertEquals(-1, RiftRules.settleTarget(3, 3, built::contains), "an empty range");
        assertEquals(-1, RiftRules.settleTarget(3, 9, c -> false));
    }

    @Test
    void aShortcutThatCanNeverLeadAnywhereIsNotOpened() {
        assertTrue(RiftRules.canLeadAnywhere(3, 6, false), "floors beyond exist");
        assertTrue(RiftRules.canLeadAnywhere(6, 6, true), "none beyond but the dungeon grows");
        assertFalse(RiftRules.canLeadAnywhere(6, 6, false), "none beyond and expansion is off: a wall");
        assertFalse(RiftRules.canLeadAnywhere(7, 6, false));
    }

    @Test
    void theFirstFreeSlotIsGivenOutAndAFullFieldIsMinusOne() {
        assertEquals(0, RiftRules.firstFreeSlot(Set.of(), 64));
        assertEquals(2, RiftRules.firstFreeSlot(Set.of(0, 1, 3), 64));
        Set<Integer> all = new HashSet<>();
        for (int i = 0; i < 64; i++) all.add(i);
        assertEquals(-1, RiftRules.firstFreeSlot(all, 64));
        all.remove(63);
        assertEquals(63, RiftRules.firstFreeSlot(all, 64), "the test cell is the one left when the others are held");
    }

    @Test
    void theSpawnerSaysWhyInTheOrderItAlwaysChecked() {
        assertEquals(RiftRules.RollGate.OK, RiftRules.rollGate(true, false, true, false, true, true, true, false, 0, 1));
        assertEquals(RiftRules.RollGate.OFF, RiftRules.rollGate(false, true, false, true, false, false, false, true, 9, 1), "the switch is first");
        assertEquals(RiftRules.RollGate.SPECTATOR, RiftRules.rollGate(true, true, false, true, false, false, false, true, 9, 1));
        assertEquals(RiftRules.RollGate.AIRBORNE, RiftRules.rollGate(true, false, false, true, false, false, false, true, 9, 1));
        assertEquals(RiftRules.RollGate.IN_POCKET, RiftRules.rollGate(true, false, true, true, false, false, false, true, 9, 1));
        assertEquals(RiftRules.RollGate.NO_SECTOR, RiftRules.rollGate(true, false, true, false, false, false, false, true, 9, 1));
        assertEquals(RiftRules.RollGate.NOT_A_FLOOR, RiftRules.rollGate(true, false, true, false, true, false, false, true, 9, 1));
        assertEquals(RiftRules.RollGate.NOT_BUILT, RiftRules.rollGate(true, false, true, false, true, true, false, true, 9, 1));
        assertEquals(RiftRules.RollGate.SAFE_ROOM, RiftRules.rollGate(true, false, true, false, true, true, true, true, 9, 1));
        assertEquals(RiftRules.RollGate.CROWDED, RiftRules.rollGate(true, false, true, false, true, true, true, false, 1, 1));
        assertEquals(RiftRules.RollGate.OK, RiftRules.rollGate(true, false, true, false, true, true, true, false, 1, 2), "maxNear 2 lets a second one open");
    }

    @Test
    void everyPlaceAPlayerIsPutDownAfterAPocketIsOutsideTheTearTheyWentInBy() {
        for (float yaw : new float[]{0f, 90f, 180f, -90f, 33f}) {
            double[][] ring = RiftRules.exitRing(100, 200, 100.5, 200.5, RiftRules.EXIT_RING, yaw);
            assertEquals(8, ring.length);
            for (double[] c : ring) {
                assertEquals(RiftRules.EXIT_RING, Math.hypot(c[0] - 100.5, c[1] - 200.5), 1e-9, "on the ring");
                assertFalse(RiftRules.inTear(100, 64, 200, c[0], 64, c[1]), "outside the tear: " + c[0] + ", " + c[1]);
            }
        }
    }

    @Test
    void theFirstPlaceIsOnTheSideThePlayerCameFrom() {
        double[][] ring = RiftRules.exitRing(0, 0, -0.9, 0.5, RiftRules.EXIT_RING, 0f);        // stood west of the middle (x = 0.5)
        assertTrue(ring[0][0] < 0.5 - 2.0, "the first candidate is on the west side: " + ring[0][0]);
        assertEquals(0.5, ring[0][1], 1e-6);
    }

    @Test
    void aPlayerStandingExactlyInTheMiddleIsPutDownBehindTheWayTheyFaced() {
        // yaw 0 faces south (+z), so behind is north (-z)
        double[][] ring = RiftRules.exitRing(0, 0, 0.5, 0.5, RiftRules.EXIT_RING, 0f);
        assertEquals(0.5, ring[0][0], 1e-6);
        assertTrue(ring[0][1] < 0.5 - 2.0, "north of the middle: " + ring[0][1]);
        // yaw 90 faces west (-x), so behind is east (+x)
        ring = RiftRules.exitRing(0, 0, 0.5, 0.5, RiftRules.EXIT_RING, 90f);
        assertTrue(ring[0][0] > 0.5 + 2.0, "east of the middle: " + ring[0][0]);
    }

    @Test
    void aClickOnThePlateOrTheBlockUnderItIsAPress() {
        int[][] buttons = {{5, 96, 7}, {-6, 96, 7}, {5, 96, -6}, {-6, 96, -6}};
        assertEquals(0, RiftRules.plateHit(buttons, 5, 96, 7), "the button");
        assertEquals(0, RiftRules.plateHit(buttons, 5, 95, 7), "the coloured block under it");
        assertEquals(3, RiftRules.plateHit(buttons, -6, 95, -6));
        assertEquals(-1, RiftRules.plateHit(buttons, 5, 97, 7), "above");
        assertEquals(-1, RiftRules.plateHit(buttons, 5, 94, 7), "the floor under the plate");
        assertEquals(-1, RiftRules.plateHit(buttons, 6, 96, 7), "the next block");
        assertEquals(-1, RiftRules.plateHit(new int[0][], 5, 96, 7));
    }

    @Test
    void anEmptyPocketCollapsesAfterNinetySecondsOrTwentyAfterItWasCleared() {
        assertFalse(RiftRules.collapseEmpty(false, true, 99999, 99999), "someone is inside");
        assertFalse(RiftRules.collapseEmpty(true, false, 20 * 90, 0), "the party may still be coming");
        assertTrue(RiftRules.collapseEmpty(true, false, 20 * 90 + 1, 0));
        assertFalse(RiftRules.collapseEmpty(true, true, 20 * 30, 20 * 20), "cleared 20 s ago exactly: not yet");
        assertTrue(RiftRules.collapseEmpty(true, true, 20 * 30, 20 * 20 + 1), "cleared and left empty: frees its cell soon");
        assertFalse(RiftRules.collapseEmpty(true, false, 20 * 30, 20 * 999), "not cleared: the clearing time means nothing");
    }

    @Test
    void theCompassNamesTheEightDirections() {
        assertEquals("south", RiftRules.compass(0, 10));
        assertEquals("north", RiftRules.compass(0, -10));
        assertEquals("east", RiftRules.compass(10, 0));
        assertEquals("west", RiftRules.compass(-10, 0));
        assertEquals("north-east", RiftRules.compass(10, -10));
        assertEquals("south-west", RiftRules.compass(-10, 10));
    }

    @Test
    void theNewConstantsAreSane() {
        assertTrue(RiftRules.FORM_PATIENCE_TICKS >= 20 * 10 && RiftRules.FORM_PATIENCE_TICKS < 20 * 240, "the patience is shorter than a rift's life (240 s)");
        assertTrue(RiftRules.FAIL_LIMIT >= 1);
        assertTrue(RiftRules.EXIT_RING * Math.sqrt(0.5) > RiftRules.TEAR_HALF, "even the diagonal points of the ring are outside the tear");
        assertTrue(RiftRules.BEACON_EVERY >= 20, "no more than one hint a second");
    }
}
