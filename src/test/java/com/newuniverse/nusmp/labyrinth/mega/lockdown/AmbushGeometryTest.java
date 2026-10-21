package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.enc.SpawnLayout;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: an ambush is only an ambush if it is AHEAD. These tests lay out a small floor (the player's own room, a corridor ahead, a hall behind, a safe room ahead, a room
 * to the side) and pin that the squad is placed in the corridor the player is running into and not behind or beside, never in a safe room or the room the player is
 * in, within the distance band, with a long corridor giving several cells along its length, that the cone can be widened, and that the choice is deterministic.
 */
class AmbushGeometryTest {
    private static SpawnLayout.RoomBox room(int id, RoomKind kind, int minX, int minZ, int maxX, int maxZ) {
        return new SpawnLayout.RoomBox(id, kind, minX, 56, minZ, maxX, 104, maxZ);
    }

    /** The player is in room 0 (x -9..9), running east (+x). */
    private static SpawnLayout floor() {
        List<SpawnLayout.RoomBox> rooms = List.of(
                room(0, RoomKind.HALL, -9, -9, 9, 9),                       // the player's own room
                room(1, RoomKind.CORRIDOR, 12, -2, 60, 2),                  // the corridor ahead: 49 long
                room(2, RoomKind.HALL, -60, -9, -30, 9),                    // a hall behind
                room(3, RoomKind.SAFE, 30, 20, 45, 35),                     // a safe room ahead and to the side
                room(4, RoomKind.HALL, -9, 30, 9, 50));                     // a hall to the side (north-south axis)
        return new SpawnLayout(rooms, List.of(), List.of());
    }

    @Test
    void theSquadIsInTheCorridorAheadAndNotBehindOrBeside() {
        List<AmbushGeometry.Pick> p = AmbushGeometry.ahead(floor(), 0.0, 0.0, 1.0, 0.0, 0, 14.0, 40.0, 0.35, 4, 8);
        assertFalse(p.isEmpty());
        for (AmbushGeometry.Pick k : p) {
            assertEquals(1, k.roomId(), "every pick is in the corridor ahead: " + k);
            assertTrue(k.x() >= 14, "ahead: " + k);
        }
    }

    @Test
    void aLongCorridorGivesSeveralCellsAlongItsLength() {
        List<AmbushGeometry.Pick> p = AmbushGeometry.ahead(floor(), 0.0, 0.0, 1.0, 0.0, 0, 14.0, 40.0, 0.35, 4, 8);
        assertTrue(p.size() >= 3, "got " + p.size());
        for (int i = 0; i < p.size(); i++) {
            for (int j = i + 1; j < p.size(); j++) {
                assertTrue(Math.hypot(p.get(i).x() - p.get(j).x(), p.get(i).z() - p.get(j).z()) >= 4.0, "picks at least the separation apart");
            }
        }
    }

    @Test
    void aSafeRoomAndTheOwnRoomAreNeverChosen() {
        for (double[] heading : new double[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {0, 0}}) {
            List<AmbushGeometry.Pick> p = AmbushGeometry.ahead(floor(), 0.0, 0.0, heading[0], heading[1], 0, 8.0, 80.0, -1.0, 2, 50);
            for (AmbushGeometry.Pick k : p) {
                assertTrue(k.roomId() != 3, "a safe room");
                assertTrue(k.roomId() != 0, "the player's own room");
            }
        }
    }

    @Test
    void theDistanceBandHolds() {
        for (AmbushGeometry.Pick k : AmbushGeometry.ahead(floor(), 0.0, 0.0, 1.0, 0.0, 0, 20.0, 30.0, 0.0, 2, 50)) {
            double d = Math.hypot(k.x() + 0.5, k.z() + 0.5);
            assertTrue(d >= 20.0 && d <= 30.0, "distance " + d + " for " + k);
        }
    }

    @Test
    void theOtherWayRunsIntoTheHallBehind() {
        List<AmbushGeometry.Pick> p = AmbushGeometry.ahead(floor(), 0.0, 0.0, -1.0, 0.0, 0, 14.0, 70.0, 0.35, 4, 8);
        assertFalse(p.isEmpty());
        for (AmbushGeometry.Pick k : p) assertEquals(2, k.roomId());
    }

    @Test
    void theConeCanBeWidenedButNotToTheRear() {
        // heading east: the hall to the north (z 30..50, centred at x 0) is straight 90 degrees off
        List<AmbushGeometry.Pick> wide = AmbushGeometry.ahead(floor(), 0.0, 0.0, 1.0, 0.0, 0, 28.0, 60.0, -0.25, 4, 20);
        for (AmbushGeometry.Pick k : wide) assertTrue(k.roomId() != 2, "the hall straight behind is never in even the widest cone");
        boolean side = false;
        for (AmbushGeometry.Pick k : wide) side |= k.roomId() == 4;
        assertTrue(side, "a wider cone reaches the room beside the way");
    }

    @Test
    void aStandingPlayerIsAmbushedWhereverTheRoomsAre() {
        List<AmbushGeometry.Pick> p = AmbushGeometry.ahead(floor(), 0.0, 0.0, 0.0, 0.0, 0, 14.0, 60.0, 0.35, 4, 8);
        assertFalse(p.isEmpty(), "no heading means any way");
    }

    @Test
    void theChoiceIsDeterministicAndRanked() {
        List<AmbushGeometry.Pick> a = AmbushGeometry.ahead(floor(), 0.0, 0.0, 1.0, 0.0, 0, 14.0, 40.0, 0.35, 4, 8);
        List<AmbushGeometry.Pick> b = AmbushGeometry.ahead(floor(), 0.0, 0.0, 1.0, 0.0, 0, 14.0, 40.0, 0.35, 4, 8);
        assertEquals(a, b);
        for (int i = 1; i < a.size(); i++) assertTrue(a.get(i - 1).score() >= a.get(i).score(), "best first");
    }

    @Test
    void anEmptyFloorGivesNothing() {
        assertTrue(AmbushGeometry.ahead(new SpawnLayout(List.of(), List.of(), List.of()), 0, 0, 1, 0, -1, 14, 40, 0.35, 4, 8).isEmpty());
        assertTrue(AmbushGeometry.ahead(floor(), 0, 0, 1, 0, 0, 14, 40, 0.35, 4, 0).isEmpty(), "want = 0");
    }
}
