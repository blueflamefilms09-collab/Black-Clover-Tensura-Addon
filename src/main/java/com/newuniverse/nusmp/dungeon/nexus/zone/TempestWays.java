package com.newuniverse.nusmp.dungeon.nexus.zone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The ways into the Tempest Mirage. The village is a sealed cavern in a cave world that is flooded below sea level, and the Dungeon's stone
 * may not be dug by anyone (DungeonConfig.UNBREAKABLE), so a village nobody can enter would be of no use. Each of its four sealed gates is
 * opened by the mod itself, through the plug in the shell and then through the rock outside, to the nearest cave, and ends in an open iron
 * door: a door's cell cannot hold water, so the flooded caves stay out while a swimmer can come in (the idea of dungeon/city/CityWays). This
 * class only finds the path outside the door: the cheapest passage, one block wide and two high, from the cell beyond the door to a cave with
 * room in it, where the cost is the rock to be cut and a turn costs a little. It is pure: the world is reached through a {@link Probe}.
 */
final class TempestWays {
    private TempestWays() {}

    /** What a probe says of a cell: rock (anything that is not air or water), a cave (air or water), not loaded, or not to be touched. */
    static final int SOLID = 0, OPEN = 1, UNKNOWN = 2, BLOCKED = 3;
    /** The most rock that may be cut outside the door, the furthest a passage may wander from the door (blocks along both axes), and the open cells needed round the end of a passage. */
    static final int REACH = 40, SPAN = 60, MIN_CAVE = 10;
    /** What entering a cell costs: rock is dear enough that no run of cave cells or turns ever outweighs one block of it. */
    private static final int ROCK = 1000, CAVE = 1, TURN = 3;
    private static final int[][] DIRS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    /** The world, read at one cell. */
    interface Probe { int at(int x, int y, int z); }

    /** A passage: the cells {x, z} from the one beyond the door to the cave, and how many of them are rock. */
    record Route(List<int[]> cells, int dug) {}

    /**
     * The state of the column (x, z) for a passage that is two high from y1: BLOCKED or UNKNOWN if either cell is, OPEN if both are open,
     * SOLID if rock is in the way.
     */
    static int column(Probe probe, int x, int z, int y1) {
        int a = probe.at(x, y1, z), b = probe.at(x, y1 + 1, z);
        if (a == BLOCKED || b == BLOCKED) return BLOCKED;
        if (a == UNKNOWN || b == UNKNOWN) return UNKNOWN;
        return a == OPEN && b == OPEN ? OPEN : SOLID;
    }

    /** Does a passage that ends here come out in a cave, not a crack? Counts the open cells in the 3 x 3 x 4 block round the end. */
    static boolean room(Probe probe, int x, int z, int y1) {
        int open = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int y = y1 - 1; y <= y1 + 2; y++) if (probe.at(x + dx, y, z + dz) == OPEN) open++;
        return open >= MIN_CAVE;
    }

    private record Node(long cost, int state, long order) {}

    /**
     * The cheapest passage from the cell outside a door at (doorX, doorZ) that looks in the direction (dx, dz), with the floor of the passage
     * at y1, or null if there is none within {@link #REACH} cells of rock and {@link #SPAN} cells of the door (or if the caves round it are not
     * loaded). A state is a cell and the direction the passage entered it in, so that a turn can cost.
     */
    static Route route(int doorX, int doorZ, int dx, int dz, int y1, Probe probe) {
        final int w = 2 * SPAN + 1, bx = doorX - SPAN, bz = doorZ - SPAN;
        int startX = doorX + dx, startZ = doorZ + dz;
        int outward = 0;
        for (int i = 0; i < 4; i++) if (DIRS[i][0] == dx && DIRS[i][1] == dz) outward = i;
        byte[] kind = new byte[w * w];
        Arrays.fill(kind, (byte) -1);
        long[] best = new long[w * w * 4];
        Arrays.fill(best, Long.MAX_VALUE);
        int[] from = new int[w * w * 4];
        Arrays.fill(from, -1);

        int startCell = (startX - bx) * w + (startZ - bz);
        kind[startCell] = (byte) column(probe, startX, startZ, y1);
        if (kind[startCell] == UNKNOWN || kind[startCell] == BLOCKED) return null;
        PriorityQueue<Node> queue = new PriorityQueue<>((a, b) -> a.cost != b.cost ? Long.compare(a.cost, b.cost) : Long.compare(a.order, b.order));
        long order = 0;
        int startState = startCell * 4 + outward;
        best[startState] = kind[startCell] == OPEN ? CAVE : ROCK;
        queue.add(new Node(best[startState], startState, order++));
        int[] pref = {0, 1, 3, 2};                                            // straight on first, then to either side, then back: equal costs keep that order
        while (!queue.isEmpty()) {
            Node n = queue.poll();
            if (n.cost > best[n.state]) continue;
            int cell = n.state / 4, dir = n.state & 3, x = bx + cell / w, z = bz + cell % w;
            if (kind[cell] == OPEN && room(probe, x, z, y1)) return build(from, n.state, kind, w, bx, bz);
            for (int p : pref) {
                int d = (outward + p) & 3;
                int nx = x + DIRS[d][0], nz = z + DIRS[d][1];
                if (Math.abs(nx - doorX) + Math.abs(nz - doorZ) > SPAN) continue;
                int nc = (nx - bx) * w + (nz - bz);
                if (kind[nc] == -1) kind[nc] = (byte) column(probe, nx, nz, y1);
                if (kind[nc] == UNKNOWN || kind[nc] == BLOCKED) continue;
                long cost = n.cost + (kind[nc] == OPEN ? CAVE : ROCK) + (d == dir ? 0 : TURN);
                if (cost >= (long) ROCK * (REACH + 1)) continue;
                int ns = nc * 4 + d;
                if (cost < best[ns]) {
                    best[ns] = cost;
                    from[ns] = n.state;
                    queue.add(new Node(cost, ns, order++));
                }
            }
        }
        return null;
    }

    private static Route build(int[] from, int end, byte[] kind, int w, int bx, int bz) {
        List<int[]> cells = new ArrayList<>();
        int dug = 0;
        for (int s = end; s != -1; s = from[s]) {
            int cell = s / 4;
            cells.add(new int[]{bx + cell / w, bz + cell % w});
            if (kind[cell] != OPEN) dug++;
        }
        Collections.reverse(cells);
        return new Route(cells, dug);
    }
}
