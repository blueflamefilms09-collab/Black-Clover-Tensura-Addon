package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.zone.UnderworldWays.Probe;
import com.newuniverse.nusmp.dungeon.nexus.zone.UnderworldWays.Route;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static com.newuniverse.nusmp.dungeon.nexus.zone.UnderworldWays.*;
import static org.junit.jupiter.api.Assertions.*;

/** The passages from the sealed gates of the Underworld Gate to the caves round it. */
class UnderworldWaysTest {
    /** A world of rock with a cave (a box of open cells) in it. */
    private static Probe rockWithCave(int x1, int z1, int x2, int z2, int y1) {
        return (x, y, z) -> x >= x1 && x <= x2 && z >= z1 && z <= z2 && y >= y1 - 1 && y <= y1 + 3 ? OPEN : SOLID;
    }

    private static int[] last(Route r) { return r.cells().get(r.cells().size() - 1); }

    @Test
    void aCaveCloseByIsReachedStraightThroughTheRock() {
        // the door is at (100, 100) and looks east (+x); the cave starts 6 cells beyond it
        Route r = UnderworldWays.route(100, 100, 1, 0, -55, rockWithCave(107, 94, 120, 106, -55));
        assertNotNull(r);
        assertEquals(101, r.cells().get(0)[0], "the passage begins at the cell beyond the door");
        assertEquals(100, r.cells().get(0)[1]);
        assertEquals(6, r.dug(), "x = 101 to 106 is rock");
        for (int i = 1; i < r.cells().size(); i++) {
            int[] a = r.cells().get(i - 1), b = r.cells().get(i);
            assertEquals(1, Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]), "a passage is a chain of neighbouring cells");
        }
        assertTrue(last(r)[0] >= 107, "it ends in the cave");
    }

    @Test
    void aCaveOnTheFarSideIsReachedByTurning() {
        // the door looks east, the cave lies to the north
        Route r = UnderworldWays.route(100, 100, 1, 0, -55, rockWithCave(98, 80, 104, 92, -55));
        assertNotNull(r);
        assertTrue(r.dug() >= 8 && r.dug() <= REACH);
        assertTrue(last(r)[1] <= 92);
    }

    @Test
    void noCaveWithinReachMeansNoPassage() {
        assertNull(UnderworldWays.route(100, 100, 1, 0, -55, (x, y, z) -> SOLID));
        assertNull(UnderworldWays.route(100, 100, 1, 0, -55, rockWithCave(100 + REACH + 6, 90, 140, 110, -55)), "too far");
    }

    @Test
    void aSealedPocketIsNotACave() {
        // a single open cell is not a cave: there must be room round the end of the passage
        Probe pocket = (x, y, z) -> x == 105 && z == 100 && y >= -55 && y <= -54 ? OPEN : SOLID;
        assertNull(UnderworldWays.route(100, 100, 1, 0, -55, pocket));
    }

    @Test
    void anUnloadedOrForbiddenStartMeansNoPassage() {
        assertNull(UnderworldWays.route(100, 100, 1, 0, -55, (x, y, z) -> UNKNOWN));
        assertNull(UnderworldWays.route(100, 100, 1, 0, -55, (x, y, z) -> BLOCKED));
    }

    @Test
    void aPassageNeverEntersForbiddenOrUnloadedCells() {
        Probe cave = rockWithCave(107, 94, 120, 106, -55);
        Set<Long> forbidden = new HashSet<>();
        for (int z = 97; z <= 103; z++) forbidden.add(((long) 104 << 32) | (z & 0xFFFFFFFFL));          // a wall of forbidden cells across the straight line
        Probe walled = (x, y, z) -> forbidden.contains(((long) x << 32) | (z & 0xFFFFFFFFL)) ? BLOCKED : cave.at(x, y, z);
        Route r = UnderworldWays.route(100, 100, 1, 0, -55, walled);
        assertNotNull(r, "it goes round");
        for (int[] c : r.cells()) assertFalse(forbidden.contains(((long) c[0] << 32) | (c[1] & 0xFFFFFFFFL)), "cell " + c[0] + "," + c[1]);
        Route straight = UnderworldWays.route(100, 100, 1, 0, -55, cave);
        assertTrue(r.dug() >= straight.dug());
    }

    @Test
    void anOpenStartIsAlreadyACave() {
        Route r = UnderworldWays.route(100, 100, 1, 0, -55, (x, y, z) -> OPEN);
        assertNotNull(r);
        assertEquals(1, r.cells().size());
        assertEquals(0, r.dug());
    }

    @Test
    void aRouteIsDeterministic() {
        Probe p = rockWithCave(98, 80, 104, 92, -55);
        Route a = UnderworldWays.route(100, 100, 1, 0, -55, p), b = UnderworldWays.route(100, 100, 1, 0, -55, p);
        assertEquals(a.cells().size(), b.cells().size());
        for (int i = 0; i < a.cells().size(); i++) assertArrayEquals(a.cells().get(i), b.cells().get(i));
    }

    @Test
    void theColumnAndRoomTestsReadTwoHighAndRoundTheEnd() {
        Probe p = (x, y, z) -> y == -55 ? OPEN : (y == -54 ? SOLID : BLOCKED);
        assertEquals(SOLID, UnderworldWays.column(p, 0, 0, -55), "one open, one rock");
        assertEquals(BLOCKED, UnderworldWays.column(p, 0, 0, -54), "the cell above is forbidden");
        assertEquals(OPEN, UnderworldWays.column((x, y, z) -> OPEN, 0, 0, -55));
        assertEquals(UNKNOWN, UnderworldWays.column((x, y, z) -> UNKNOWN, 0, 0, -55));
        assertTrue(UnderworldWays.room((x, y, z) -> OPEN, 0, 0, -55));
        assertFalse(UnderworldWays.room((x, y, z) -> SOLID, 0, 0, -55));
    }
}
