package com.newuniverse.nusmp.labyrinth.mega.cyclic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the property tests of the cyclic layer need thousands of dungeons that look like what the generator makes, without a catalog or a game: rooms are
 * rectangles of module cells (1x1 up to 3x3), each boundary module may carry a doorway, and the dungeon is GROWN AS A TREE from the entrance by attaching a
 * room to open doorways until a room budget is spent (exactly the shape of {@code MegaAssembler}'s breadth-first fill). What is left open at the end is the
 * material the loop pass works on. The generator is deliberately a little nastier than the real one (single-door rooms are common, the region is crowded) so a
 * pass that works here is not working by luck.
 */
final class SynthDungeon {
    final int w, h;
    final RoomGraph graph = new RoomGraph();
    final List<int[]> rects = new ArrayList<>();                       // per room: {x, z, w, d}
    final Map<Cell, Integer> owner = new HashMap<>();
    final List<LoopPlanner.Door> open = new ArrayList<>();
    int entrance, stairs = -1;

    SynthDungeon(int w, int h) {
        this.w = w;
        this.h = h;
    }

    boolean inside(Cell c) { return c.x() >= 0 && c.z() >= 0 && c.x() < w && c.z() < h; }

    boolean free(Cell c) { return inside(c) && !owner.containsKey(c); }

    private static final int[][] SIZES = {{1, 1}, {1, 1}, {1, 2}, {2, 1}, {2, 2}, {1, 3}, {3, 1}, {3, 3}, {2, 3}};
    private static final RoomCategory[] CATS = {RoomCategory.COMBAT, RoomCategory.COMBAT, RoomCategory.PUZZLE_HAZARD, RoomCategory.LORE, RoomCategory.LORE, RoomCategory.TRANSITION,
            RoomCategory.TRANSITION, RoomCategory.TRANSITION, RoomCategory.TRANSITION, RoomCategory.SAFE};

    /** A grown dungeon of about {@code rooms} rooms in a region of {@code w x h} modules. */
    static SynthDungeon grow(long seed, int w, int h, int rooms) {
        Rng rng = new Rng(Rng.mix(seed ^ 0xABCDEF123L));
        SynthDungeon d = new SynthDungeon(w, h);
        Cell start = new Cell(0, h / 2);
        d.entrance = d.addRoom(start.x(), start.z(), 1, 1, RoomCategory.SAFE, Roles.ENTRANCE, rng, new LoopPlanner.Door(-1, start, Dir.WEST), 0.0);
        d.open.add(new LoopPlanner.Door(d.entrance, start, Dir.EAST));                 // the arrival stairwell has exactly one doorway, and it leads into the floor
        ArrayDeque<LoopPlanner.Door> queue = new ArrayDeque<>(d.open);
        d.open.clear();
        d.open.addAll(queue);
        int safeCount = 0;
        int guard = 0;
        while (!queue.isEmpty() && d.graph.size() < rooms && guard++ < 5000) {
            LoopPlanner.Door door = queue.poll();
            if (!d.open.contains(door)) continue;
            Cell f = door.front();
            if (!d.free(f)) continue;
            for (int attempt = 0; attempt < 10; attempt++) {
                int[] sz = SIZES[rng.nextInt(SIZES.length)];
                int ox = f.x() - rng.nextInt(sz[0]), oz = f.z() - rng.nextInt(sz[1]);
                if (!d.fits(ox, oz, sz[0], sz[1])) continue;
                RoomCategory cat = CATS[rng.nextInt(CATS.length)];
                int role = 0;
                if (cat == RoomCategory.SAFE) {
                    if (safeCount >= 2) cat = RoomCategory.COMBAT;
                    else {
                        role = Roles.SAFE;
                        safeCount++;
                    }
                }
                int before = d.open.size();
                int id = d.addRoom(ox, oz, sz[0], sz[1], cat, role, rng, new LoopPlanner.Door(door.room(), door.cell(), door.dir()), 0.62);
                d.graph.addEdge(door.room(), id);
                d.open.remove(door);
                for (int i = before; i < d.open.size(); i++) queue.add(d.open.get(i));
                break;
            }
        }
        // the free loops the real assembler also finds: two open doorways that line up
        d.closeAligned();
        // the forward stairs: a leaf-ish room far from the entrance
        int[] dist = d.graph.distances(d.entrance);
        int far = d.entrance;
        for (int i = 0; i < dist.length; i++) if (dist[i] > dist[far]) far = i;
        d.stairs = far;
        if (far != d.entrance) d.graph.addRole(far, Roles.STAIRS);
        return d;
    }

    boolean fits(int x, int z, int rw, int rd) {
        for (int i = 0; i < rw; i++) for (int j = 0; j < rd; j++) if (!free(new Cell(x + i, z + j))) return false;
        return true;
    }

    /** Adds a room, registers its cells and gives its boundary modules a doorway with probability {@code p} (the attach doorway always exists). */
    int addRoom(int x, int z, int rw, int rd, RoomCategory cat, int role, Rng rng, LoopPlanner.Door attachedFrom, double p) {
        int id = graph.addNode(cat, role);
        rects.add(new int[] {x, z, rw, rd});
        for (int i = 0; i < rw; i++) for (int j = 0; j < rd; j++) owner.put(new Cell(x + i, z + j), id);
        Cell attach = attachedFrom.front();
        for (int i = 0; i < rw; i++) {
            for (int j = 0; j < rd; j++) {
                Cell c = new Cell(x + i, z + j);
                for (Dir dir : Dir.all()) {
                    Cell n = c.step(dir);
                    boolean boundary = !(n.x() >= x && n.x() < x + rw && n.z() >= z && n.z() < z + rd);
                    if (!boundary) continue;
                    boolean isAttach = c.equals(attach) && dir == attachedFrom.dir().opposite();
                    if (isAttach) continue;                                       // the attach doorway is consumed by the tree edge, not open
                    if (!inside(n)) continue;
                    if (rng.nextDouble() < p) open.add(new LoopPlanner.Door(id, c, dir));
                }
            }
        }
        return id;
    }

    /** Connects every pair of open doorways that line up (what {@code MegaAssembler.loopsNow} does). */
    void closeAligned() {
        Set<Integer> done = new HashSet<>();
        for (int i = 0; i < open.size(); i++) {
            for (int j = i + 1; j < open.size(); j++) {
                if (done.contains(i) || done.contains(j)) continue;
                LoopPlanner.Door a = open.get(i), b = open.get(j);
                if (a.room() != b.room() && SocketRules.aligned(a, b)) {
                    graph.addEdge(a.room(), b.room());
                    done.add(i);
                    done.add(j);
                }
            }
        }
        List<LoopPlanner.Door> keep = new ArrayList<>();
        for (int i = 0; i < open.size(); i++) if (!done.contains(i)) keep.add(open.get(i));
        open.clear();
        open.addAll(keep);
    }
}
