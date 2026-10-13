package com.newuniverse.nusmp.dungeon.nexus.zone;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/** The path from a sealed gate of the Monarch's Vault to the nearest cave. */
class MonarchWaysTest {
    private static final int Y1 = -55;

    /** A world described by the state of each column (the same for every height). */
    private static MonarchWays.Probe columns(IntBinaryOperator state) {
        return (x, y, z) -> state.applyAsInt(x, z);
    }

    private static void assertValid(MonarchWays.Route r, int doorX, int doorZ, int dx, int dz, MonarchWays.Probe p) {
        assertNotNull(r);
        assertFalse(r.cells().isEmpty());
        int[] first = r.cells().get(0);
        assertEquals(doorX + dx, first[0], "the passage starts just outside the door");
        assertEquals(doorZ + dz, first[1]);
        int dug = 0;
        for (int i = 0; i < r.cells().size(); i++) {
            int[] c = r.cells().get(i);
            int s = MonarchWays.column(p, c[0], c[1], Y1);
            assertTrue(s == MonarchWays.SOLID || s == MonarchWays.OPEN, "a passage only crosses rock and caves, not " + s + " at " + c[0] + "," + c[1]);
            if (s == MonarchWays.SOLID) dug++;
            if (i > 0) {
                int[] b = r.cells().get(i - 1);
                assertEquals(1, Math.abs(c[0] - b[0]) + Math.abs(c[1] - b[1]), "each step is one block");
            }
            assertTrue(Math.abs(c[0] - doorX) + Math.abs(c[1] - doorZ) <= MonarchWays.SPAN);
        }
        assertEquals(dug, r.dug());
        int[] last = r.cells().get(r.cells().size() - 1);
        assertEquals(MonarchWays.OPEN, MonarchWays.column(p, last[0], last[1], Y1));
        assertTrue(MonarchWays.room(p, last[0], last[1], Y1), "it ends in a cave with room");
    }

    @Test
    void aCaveRightOutsideNeedsNoDigging() {
        MonarchWays.Probe p = columns((x, z) -> MonarchWays.OPEN);
        MonarchWays.Route r = MonarchWays.route(0, 0, 1, 0, Y1, p);
        assertValid(r, 0, 0, 1, 0, p);
        assertEquals(0, r.dug());
        assertEquals(1, r.cells().size());
    }

    @Test
    void rockIsCutInAStraightLineToTheCave() {
        MonarchWays.Probe p = columns((x, z) -> x >= 7 ? MonarchWays.OPEN : MonarchWays.SOLID);
        MonarchWays.Route r = MonarchWays.route(0, 0, 1, 0, Y1, p);
        assertValid(r, 0, 0, 1, 0, p);
        assertEquals(6, r.dug());
        assertEquals(7, r.cells().size());
        for (int[] c : r.cells()) assertEquals(0, c[1], "straight on");
    }

    @Test
    void aCaveToTheSideIsReachedByTurning() {
        MonarchWays.Probe p = columns((x, z) -> z >= 6 ? MonarchWays.OPEN : MonarchWays.SOLID);
        MonarchWays.Route r = MonarchWays.route(10, 0, 1, 0, Y1, p);
        assertValid(r, 10, 0, 1, 0, p);
        assertEquals(6, r.dug());
    }

    @Test
    void theNearerOfTwoCavesWins() {
        MonarchWays.Probe p = columns((x, z) -> x >= 30 ? MonarchWays.OPEN : z <= -4 ? MonarchWays.OPEN : MonarchWays.SOLID);
        MonarchWays.Route r = MonarchWays.route(0, 0, 1, 0, Y1, p);
        assertValid(r, 0, 0, 1, 0, p);
        assertEquals(4, r.dug(), "four blocks of rock to the north cave, not twenty-nine to the east one");
    }

    @Test
    void aCrackIsNotACave() {
        // a single open line, one wide: not enough room round its end; the real cave lies further
        MonarchWays.Probe p = (x, y, z) -> x >= 15 ? MonarchWays.OPEN : z == 0 && x >= 3 && (y == Y1 || y == Y1 + 1) ? MonarchWays.OPEN : MonarchWays.SOLID;
        MonarchWays.Route r = MonarchWays.route(0, 0, 1, 0, Y1, p);
        assertValid(r, 0, 0, 1, 0, p);
        int[] last = r.cells().get(r.cells().size() - 1);
        assertTrue(last[0] >= 14, "it goes on to the cave, ending at " + last[0]);
        assertFalse(MonarchWays.room(p, 5, 0, Y1));
    }

    @Test
    void noCaveWithinReachMeansNoWay() {
        final int reach = MonarchWays.REACH;
        assertNull(MonarchWays.route(0, 0, 1, 0, Y1, columns((x, z) -> MonarchWays.SOLID)));
        assertNotNull(MonarchWays.route(0, 0, 1, 0, Y1, columns((x, z) -> x >= reach + 1 ? MonarchWays.OPEN : MonarchWays.SOLID)), reach + " blocks of rock is the limit");
        assertNull(MonarchWays.route(0, 0, 1, 0, Y1, columns((x, z) -> x >= reach + 2 ? MonarchWays.OPEN : MonarchWays.SOLID)), "one more is too many");
        assertTrue(MonarchWays.REACH < MonarchWays.SPAN, "a passage can be longer than the rock it cuts");
    }

    @Test
    void unloadedOrForbiddenGroundIsNeverCrossed() {
        assertNull(MonarchWays.route(0, 0, 1, 0, Y1, columns((x, z) -> x >= 20 ? MonarchWays.OPEN : x >= 5 ? MonarchWays.UNKNOWN : MonarchWays.SOLID)), "the way would cross unloaded chunks");
        assertNull(MonarchWays.route(0, 0, 1, 0, Y1, columns((x, z) -> x >= 20 ? MonarchWays.OPEN : x >= 5 ? MonarchWays.BLOCKED : MonarchWays.SOLID)), "and ground that must not be touched");
        assertNull(MonarchWays.route(0, 0, 1, 0, Y1, columns((x, z) -> x == 1 ? MonarchWays.UNKNOWN : MonarchWays.OPEN)), "the cell outside the door must be known");
        // a bar of forbidden ground with a gap: the way goes through the gap
        MonarchWays.Probe gap = columns((x, z) -> x >= 20 ? MonarchWays.OPEN : (x == 8 && z != 3) ? MonarchWays.BLOCKED : MonarchWays.SOLID);
        MonarchWays.Route r = MonarchWays.route(0, 0, 1, 0, Y1, gap);
        assertValid(r, 0, 0, 1, 0, gap);
        assertTrue(r.cells().stream().anyMatch(c -> c[0] == 8 && c[1] == 3));
    }

    @Test
    void passagesGoOutInAnyDirection() {
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            MonarchWays.Probe p = columns((x, z) -> MonarchWays.SOLID);
            int dx = d[0], dz = d[1];
            MonarchWays.Probe cave = columns((x, z) -> (x - 100) * dx + (z - 200) * dz >= 9 ? MonarchWays.OPEN : MonarchWays.SOLID);
            MonarchWays.Route r = MonarchWays.route(100, 200, dx, dz, Y1, cave);
            assertValid(r, 100, 200, dx, dz, cave);
            assertEquals(8, r.dug());
            assertNull(MonarchWays.route(100, 200, dx, dz, Y1, p));
        }
    }

    @Test
    void theRouteIsTheCheapestInRockOnRandomWorlds() {
        int found = 0, none = 0;
        for (int seed = 0; seed < 300; seed++) {
            final int sd = seed;
            MonarchWays.Probe p = columns((x, z) -> {
                int h = MonarchRng.hash(sd, Math.floorDiv(x, 3), Math.floorDiv(z, 3), 5) % 100;
                if (x == 1 && z == 0) return MonarchWays.SOLID;
                return h < 58 ? MonarchWays.SOLID : h < 88 ? MonarchWays.OPEN : h < 94 ? MonarchWays.UNKNOWN : MonarchWays.BLOCKED;
            });
            MonarchWays.Route r = MonarchWays.route(0, 0, 1, 0, Y1, p);
            int best = reference(p);
            if (best < 0 || best > MonarchWays.REACH) {
                assertNull(r, "seed " + seed);
                none++;
            } else {
                assertValid(r, 0, 0, 1, 0, p);
                assertEquals(best, r.dug(), "seed " + seed);
                found++;
            }
        }
        assertTrue(found > 100 && none >= 1, found + " found, " + none + " without");
    }

    /** The least rock to cut to reach a cave with room, by a plain 0-1 breadth-first search over the grid (no turns, no tie-breaks); -1 if there is none. */
    private static int reference(MonarchWays.Probe p) {
        int w = 2 * MonarchWays.SPAN + 1, off = MonarchWays.SPAN;
        int inf = Integer.MAX_VALUE / 2;
        int[][] d = new int[w][w];
        for (int[] row : d) java.util.Arrays.fill(row, inf);
        int s0 = MonarchWays.column(p, 1, 0, Y1);
        if (s0 == MonarchWays.UNKNOWN || s0 == MonarchWays.BLOCKED) return -1;
        d[1 + off][off] = s0 == MonarchWays.OPEN ? 0 : 1;
        java.util.ArrayDeque<int[]> dq = new java.util.ArrayDeque<>();
        dq.add(new int[]{1, 0});
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!dq.isEmpty()) {
            int[] c = dq.pollFirst();
            for (int[] k : dirs) {
                int nx = c[0] + k[0], nz = c[1] + k[1];
                if (Math.abs(nx) + Math.abs(nz) > MonarchWays.SPAN) continue;
                int s = MonarchWays.column(p, nx, nz, Y1);
                if (s == MonarchWays.UNKNOWN || s == MonarchWays.BLOCKED) continue;
                int step = s == MonarchWays.OPEN ? 0 : 1;
                int cost = d[c[0] + off][c[1] + off] + step;
                if (cost < d[nx + off][nz + off]) {
                    d[nx + off][nz + off] = cost;
                    if (step == 0) dq.addFirst(new int[]{nx, nz});
                    else dq.addLast(new int[]{nx, nz});
                }
            }
        }
        int best = inf;
        for (int x = -off; x <= off; x++) for (int z = -off; z <= off; z++) {
            if (d[x + off][z + off] >= best || Math.abs(x) + Math.abs(z) > MonarchWays.SPAN) continue;
            if (MonarchWays.column(p, x, z, Y1) == MonarchWays.OPEN && MonarchWays.room(p, x, z, Y1)) best = d[x + off][z + off];
        }
        return best >= inf ? -1 : best;
    }
}
