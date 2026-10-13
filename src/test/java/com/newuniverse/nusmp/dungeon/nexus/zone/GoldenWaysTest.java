package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.zone.GoldenWays.Probe;
import com.newuniverse.nusmp.dungeon.nexus.zone.GoldenWays.Route;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The ways out of the Golden City's garden: the cheapest passage from a gate to a cave, through rock the Dungeon will not let anybody dig. */
class GoldenWaysTest {
    private static final int Y1 = -53;

    /** Rock everywhere except the boxes it is told are caves, and the ones it is told are not to be touched or are not loaded. */
    private static final class World implements Probe {
        final Set<String> caves = new HashSet<>(), blocked = new HashSet<>(), unknown = new HashSet<>();

        private static void fill(Set<String> into, int x1, int z1, int x2, int z2) {
            for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) for (int y = Y1 - 2; y <= Y1 + 3; y++) into.add(x + "," + y + "," + z);
        }

        void cave(int x1, int z1, int x2, int z2) { fill(caves, x1, z1, x2, z2); }

        void block(int x1, int z1, int x2, int z2) { fill(blocked, x1, z1, x2, z2); }

        void unload(int x1, int z1, int x2, int z2) { fill(unknown, x1, z1, x2, z2); }

        @Override
        public int at(int x, int y, int z) {
            String k = x + "," + y + "," + z;
            if (unknown.contains(k)) return GoldenWays.UNKNOWN;
            if (blocked.contains(k)) return GoldenWays.BLOCKED;
            return caves.contains(k) ? GoldenWays.OPEN : GoldenWays.SOLID;
        }
    }

    private static void assertContiguous(Route r, int doorX, int doorZ, int dx, int dz) {
        int[] first = r.cells().get(0);
        assertEquals(doorX + dx, first[0], "the passage starts just outside the door");
        assertEquals(doorZ + dz, first[1]);
        for (int i = 1; i < r.cells().size(); i++) {
            int[] a = r.cells().get(i - 1), b = r.cells().get(i);
            assertEquals(1, Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]), "one cell at a time");
        }
    }

    @Test
    void aCaveJustOutsideNeedsNoDigging() {
        World w = new World();
        w.cave(11, -10, 30, 10);
        Route r = GoldenWays.route(10, 0, 1, 0, Y1, w);
        assertNotNull(r);
        assertEquals(0, r.dug());
        assertEquals(1, r.cells().size());
        assertContiguous(r, 10, 0, 1, 0);
    }

    @Test
    void theNearestCaveIsReachedByTheShortestDig() {
        World w = new World();
        w.cave(18, -3, 40, 3);                                  // 7 cells of rock between the first cell outside the door (11) and the cave (18)
        w.cave(-40, 30, -20, 40);                               // a farther one
        Route r = GoldenWays.route(10, 0, 1, 0, Y1, w);
        assertNotNull(r);
        assertEquals(7, r.dug(), "the rock between the door and the cave");
        assertContiguous(r, 10, 0, 1, 0);
        int[] last = r.cells().get(r.cells().size() - 1);
        assertEquals(GoldenWays.OPEN, GoldenWays.column(w, last[0], last[1], Y1), "it ends in the cave");
        assertTrue(GoldenWays.room(w, last[0], last[1], Y1), "with room in it");
        int rock = 0;
        for (int[] c : r.cells()) if (GoldenWays.column(w, c[0], c[1], Y1) != GoldenWays.OPEN) rock++;
        assertEquals(r.dug(), rock, "dug counts the rock cells");
    }

    @Test
    void aPocketIsNotACave() {
        World w = new World();
        w.cave(15, 0, 16, 0);                                   // two cells: fewer than ten open cells round its mouth
        assertNull(GoldenWays.route(10, 0, 1, 0, Y1, w), "a pocket does not count");
        w.cave(15, 0, 40, 0);                                   // a long tunnel does
        Route r = GoldenWays.route(10, 0, 1, 0, Y1, w);
        assertNotNull(r);
        assertEquals(4, r.dug());
    }

    @Test
    void nothingWithinReachGivesNothing() {
        World w = new World();
        w.cave(10 + GoldenWays.REACH + 5, -5, 10 + GoldenWays.REACH + 30, 5);
        assertNull(GoldenWays.route(10, 0, 1, 0, Y1, w), "farther than the reach");
        World near = new World();
        near.cave(10 + GoldenWays.REACH - 2, -5, 10 + GoldenWays.REACH + 30, 5);
        assertNotNull(GoldenWays.route(10, 0, 1, 0, Y1, near), "just within the reach");
    }

    @Test
    void theRouteGoesAroundWhatIsNotToBeTouched() {
        World w = new World();
        w.cave(30, -5, 45, 5);
        w.block(20, -30, 22, 6);                                // a wall across the straight way, with a gap at z = 7 .. 8
        w.block(20, 9, 22, 30);
        Route r = GoldenWays.route(10, 0, 1, 0, Y1, w);
        assertNotNull(r, "through the gap");
        assertContiguous(r, 10, 0, 1, 0);
        boolean gap = false;
        for (int[] c : r.cells()) {
            for (int y = Y1; y <= Y1 + 1; y++) assertNotEquals(GoldenWays.BLOCKED, w.at(c[0], y, c[1]), "never through a blocked cell");
            if (c[0] >= 20 && c[0] <= 22) gap = c[1] == 7 || c[1] == 8 || gap;
        }
        assertTrue(gap, "it crossed the wall at the gap");
        World shut = new World();
        shut.cave(30, -5, 45, 5);
        shut.block(20, -70, 22, 70);
        assertNull(GoldenWays.route(10, 0, 1, 0, Y1, shut), "no way round a wall that is too long");
    }

    @Test
    void aStartThatCannotBeReadOrTouchedGivesNothing() {
        World w = new World();
        w.cave(11, -10, 30, 10);
        w.unload(11, 0, 11, 0);
        assertNull(GoldenWays.route(10, 0, 1, 0, Y1, w), "the cell outside the door is not loaded");
        World b = new World();
        b.cave(11, -10, 30, 10);
        b.block(11, 0, 11, 0);
        assertNull(GoldenWays.route(10, 0, 1, 0, Y1, b), "the cell outside the door is not to be touched");
        World hole = new World();
        hole.cave(30, -10, 50, 10);
        hole.unload(12, -70, 13, 70);                           // the caves are there but the way to them crosses chunks that are not loaded
        assertNull(GoldenWays.route(10, 0, 1, 0, Y1, hole), "a way through unloaded ground is not taken");
    }

    @Test
    void everyDirectionWorksAndTheRouteStaysWithinSpanOfTheDoor() {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            World w = new World();
            int cx = 100 + d[0] * 9, cz = 50 + d[1] * 9;
            w.cave(cx - 3, cz - 3, cx + 12, cz + 12);
            w.cave(cx - 12, cz - 12, cx + 3, cz + 3);
            Route r = GoldenWays.route(100, 50, d[0], d[1], Y1, w);
            assertNotNull(r, "direction " + d[0] + "," + d[1]);
            assertContiguous(r, 100, 50, d[0], d[1]);
            for (int[] c : r.cells()) assertTrue(Math.abs(c[0] - 100) + Math.abs(c[1] - 50) <= GoldenWays.SPAN);
            Route again = GoldenWays.route(100, 50, d[0], d[1], Y1, w);
            assertEquals(r.cells().size(), again.cells().size());
            for (int i = 0; i < r.cells().size(); i++) assertArrayEquals(r.cells().get(i), again.cells().get(i), "deterministic");
        }
    }

    @Test
    void waterFilledCavesCountLikeAirOnes() {
        World w = new World();
        w.cave(11, -10, 30, 10);
        assertEquals(GoldenWays.OPEN, GoldenWays.column(w, 12, 0, Y1), "the probe says open for air and water alike (the glue maps both to OPEN)");
        assertEquals(GoldenWays.SOLID, GoldenWays.column(w, 40, 0, Y1));
        assertEquals(GoldenWays.BLOCKED, GoldenWays.column(new Probe() {
            @Override
            public int at(int x, int y, int z) { return y == Y1 + 1 ? GoldenWays.BLOCKED : GoldenWays.OPEN; }
        }, 0, 0, Y1), "either of the two cells blocked blocks the column");
    }
}
