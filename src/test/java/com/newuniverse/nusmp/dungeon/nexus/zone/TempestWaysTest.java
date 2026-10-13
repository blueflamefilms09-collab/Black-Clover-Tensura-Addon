package com.newuniverse.nusmp.dungeon.nexus.zone;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The ways into the Tempest Mirage: the cheapest cut from a gate's door to a cave, in a made-up world. */
class TempestWaysTest {
    private static boolean in(int x, int y, int z, int x1, int y1, int z1, int x2, int y2, int z2) { return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2; }

    private static void assertContiguous(TempestWays.Route r, int startX, int startZ) {
        List<int[]> c = r.cells();
        assertArrayEquals(new int[]{startX, startZ}, c.get(0), "starts beside the door");
        for (int i = 1; i < c.size(); i++) assertEquals(1, Math.abs(c.get(i)[0] - c.get(i - 1)[0]) + Math.abs(c.get(i)[1] - c.get(i - 1)[1]), "a passage is one cell wide, step by step");
    }

    @Test
    void cutsTheShortestWayToTheNearestCave() {
        TempestWays.Probe world = (x, y, z) -> in(x, y, z, 8, -1, -3, 20, 3, 3) ? TempestWays.OPEN : TempestWays.SOLID;
        TempestWays.Route r = TempestWays.route(0, 0, 1, 0, 0, world);
        assertNotNull(r);
        assertContiguous(r, 1, 0);
        assertEquals(7, r.dug(), "seven blocks of rock between the door and the cave");
        int[] last = r.cells().get(r.cells().size() - 1);
        assertEquals(8, last[0]);
        assertEquals(0, last[1]);
        for (int[] cell : r.cells()) assertEquals(0, cell[1], "straight on when nothing is gained by turning");
    }

    @Test
    void worksOnEveryAxisDirection() {
        int[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int[] d : dirs) {
            // a cave eleven cells out in the direction the door faces
            TempestWays.Probe world = (x, y, z) -> {
                int along = x * d[0] + z * d[1], across = Math.abs(x * d[1] + z * d[0]);
                return along >= 11 && across <= 3 && y >= -1 && y <= 3 ? TempestWays.OPEN : TempestWays.SOLID;
            };
            TempestWays.Route r = TempestWays.route(0, 0, d[0], d[1], 0, world);
            assertNotNull(r, "direction " + d[0] + "," + d[1]);
            assertEquals(10, r.dug());
            assertContiguous(r, d[0], d[1]);
        }
    }

    @Test
    void openAirIsPreferredToRock() {
        // a cave 6 cells out to the east, and a long open corridor north of the door leading to a cave 40 cells away
        TempestWays.Probe world = (x, y, z) -> {
            if (in(x, y, z, 6, -1, -3, 12, 3, 3)) return TempestWays.OPEN;
            if (in(x, y, z, 1, 0, -40, 1, 1, 0)) return TempestWays.OPEN;                    // the corridor, one wide, beside the door
            if (in(x, y, z, -3, -1, -50, 9, 3, -41)) return TempestWays.OPEN;                // the far cave
            return TempestWays.SOLID;
        };
        TempestWays.Route r = TempestWays.route(0, 0, 1, 0, 0, world);
        assertNotNull(r);
        assertEquals(0, r.dug(), "through the corridor: no rock at all");
        assertContiguous(r, 1, 0);
        int[] last = r.cells().get(r.cells().size() - 1);
        assertTrue(last[1] <= -38, "ends at the far cave: " + last[0] + "," + last[1]);
    }

    @Test
    void givesUpWhereThereIsNoCaveInReach() {
        assertNull(TempestWays.route(0, 0, 1, 0, 0, (x, y, z) -> TempestWays.SOLID), "all rock");
        // a cave, but more than REACH cells of rock away
        int far = TempestWays.REACH + 5;
        assertNull(TempestWays.route(0, 0, 1, 0, 0, (x, y, z) -> in(x, y, z, far, -1, -3, far + 10, 3, 3) ? TempestWays.OPEN : TempestWays.SOLID), "too deep");
        // a cave that is only a crack
        assertNull(TempestWays.route(0, 0, 1, 0, 0, (x, y, z) -> in(x, y, z, 6, 0, 0, 30, 1, 0) ? TempestWays.OPEN : TempestWays.SOLID), "a crack is not a cave");
    }

    @Test
    void doesNotCutWhatItDoesNotKnowOrMayNotTouch() {
        assertNull(TempestWays.route(0, 0, 1, 0, 0, (x, y, z) -> x == 1 ? TempestWays.UNKNOWN : TempestWays.OPEN), "unloaded beside the door");
        assertNull(TempestWays.route(0, 0, 1, 0, 0, (x, y, z) -> x == 1 ? TempestWays.BLOCKED : TempestWays.OPEN), "forbidden beside the door");
        // a wall of forbidden cells between the door and the cave, with one gap far to the north
        TempestWays.Probe world = (x, y, z) -> {
            if (x == 4 && z > -30) return TempestWays.BLOCKED;
            if (in(x, y, z, 8, -1, -3, 20, 3, 3)) return TempestWays.OPEN;
            return TempestWays.SOLID;
        };
        TempestWays.Route r = TempestWays.route(0, 0, 1, 0, 0, world);
        if (r != null) for (int[] c : r.cells()) assertFalse(c[0] == 4 && c[1] > -30, "never through a forbidden cell");
        TempestWays.Probe unknownAhead = (x, y, z) -> {
            if (x >= 4 && x <= 6) return TempestWays.UNKNOWN;
            if (x > 6) return TempestWays.OPEN;
            return TempestWays.SOLID;
        };
        TempestWays.Route u = TempestWays.route(0, 0, 1, 0, 0, unknownAhead);
        if (u != null) for (int[] c : u.cells()) assertFalse(c[0] >= 4 && c[0] <= 6, "never through an unloaded cell");
    }

    @Test
    void aPassageIsTwoHighFromTheGivenFloor() {
        // a cave whose air starts a block above the passage's floor cannot be reached: a passage is cut level, two cells high from y1
        TempestWays.Probe highCave = (x, y, z) -> in(x, y, z, 6, 1, -3, 20, 4, 3) ? TempestWays.OPEN : TempestWays.SOLID;
        assertNull(TempestWays.route(0, 0, 1, 0, 0, highCave), "no cell of the cave has both y = 0 and y = 1 open");
        TempestWays.Probe level = (x, y, z) -> in(x, y, z, 6, 0, -3, 20, 4, 3) ? TempestWays.OPEN : TempestWays.SOLID;
        TempestWays.Route r = TempestWays.route(0, 0, 1, 0, 0, level);
        assertNotNull(r);
        assertTrue(r.cells().get(r.cells().size() - 1)[0] >= 6, "ends inside the cave");
        assertEquals(TempestWays.SOLID, TempestWays.column(highCave, 8, 0, 0));
        assertEquals(TempestWays.OPEN, TempestWays.column(highCave, 8, 0, 1));
        assertEquals(TempestWays.SOLID, TempestWays.column(level, 3, 0, 0));
        assertEquals(TempestWays.UNKNOWN, TempestWays.column((x, y, z) -> y == 1 ? TempestWays.UNKNOWN : TempestWays.OPEN, 0, 0, 0));
        assertEquals(TempestWays.BLOCKED, TempestWays.column((x, y, z) -> y == 0 ? TempestWays.BLOCKED : TempestWays.UNKNOWN, 0, 0, 0));
    }

    @Test
    void aCaveNeedsRoomRoundTheEndOfThePassage() {
        TempestWays.Probe cave = (x, y, z) -> in(x, y, z, 0, -1, -1, 3, 2, 1) ? TempestWays.OPEN : TempestWays.SOLID;
        assertTrue(TempestWays.room(cave, 1, 0, 0));
        TempestWays.Probe crack = (x, y, z) -> in(x, y, z, 0, 0, 0, 20, 1, 0) ? TempestWays.OPEN : TempestWays.SOLID;
        assertFalse(TempestWays.room(crack, 5, 0, 0));
    }

    @Test
    void theSameWorldGivesTheSameRoute() {
        TempestWays.Probe world = (x, y, z) -> {
            int h = TempestRng.hash(7L, x, z, y) % 100;
            return h < 18 ? TempestWays.OPEN : TempestWays.SOLID;
        };
        TempestWays.Route a = TempestWays.route(5, 5, 0, 1, 0, world), b = TempestWays.route(5, 5, 0, 1, 0, world);
        if (a == null) {
            assertNull(b);
            return;
        }
        assertEquals(a.dug(), b.dug());
        assertEquals(a.cells().size(), b.cells().size());
        for (int i = 0; i < a.cells().size(); i++) assertArrayEquals(a.cells().get(i), b.cells().get(i));
    }
}
