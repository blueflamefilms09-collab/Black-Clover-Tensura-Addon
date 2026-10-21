package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: {@code seal_doors} barred every open column of a room's wall four rows high (the old doorway) and judged a side with more than 30 open columns a cavern. The 7 x 7
 * clearance made the doorways of a cut sector seven rows high and up to seven columns wide each, so these tests pin the two numbers that follow: the bar takes seven rows in a cut
 * room and four in an old one, and a long wall of 7-wide doorways is not mistaken for a cavern while a wall open from end to end still is.
 */
class DoorSealingTest {
    @Test
    void theBarTakesSevenRowsInACutRoomAndFourInAnOldOne() {
        assertEquals(MegaGrid.DOOR_H, DoorSealing.rows(false));
        assertEquals(4, DoorSealing.rows(false));
        assertEquals(7, DoorSealing.rows(true));
    }

    @Test
    void theOldLimitOfThirtyIsKeptForARoomThatWasNotCut() {
        for (int side : new int[] {9, 18, 27, 45, 90}) assertEquals(30, DoorSealing.sideLimit(false, side));
        assertTrue(DoorSealing.isDoorways(false, 27, 30));
        assertFalse(DoorSealing.isDoorways(false, 27, 31));
    }

    @Test
    void aLongWallOfSevenWideDoorwaysIsNotACavernInACutRoom() {
        assertTrue(DoorSealing.isDoorways(true, 27, 21), "three doorways in 27");
        assertTrue(DoorSealing.isDoorways(true, 45, 35), "five doorways in 45");
        assertFalse(DoorSealing.isDoorways(false, 45, 35), "the old limit refused them");
        assertTrue(DoorSealing.isDoorways(true, 90, 70), "ten doorways in 90");
        assertTrue(DoorSealing.sideLimit(true, 27) >= 30, "never below the old limit");
    }

    @Test
    void aWallOpenFromEndToEndIsStillACavern() {
        assertFalse(DoorSealing.isDoorways(true, 45, 43));
        assertFalse(DoorSealing.isDoorways(true, 90, 88));
        assertFalse(DoorSealing.isDoorways(false, 90, 88));
    }

    @Test
    void theBuiltInCheckOfTheSelfTestHolds() {
        assertEquals(List.of(), DoorSealing.selfCheck());
    }
}
