package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.dungeon.MawShapes.Kind;
import com.newuniverse.nusmp.dungeon.MawShapes.Mat;
import com.newuniverse.nusmp.dungeon.MawShapes.Plan;
import com.newuniverse.nusmp.dungeon.MawShapes.Put;
import com.newuniverse.nusmp.dungeon.MawShapes.Style;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Maw's manifestations (docs/living_dungeon.md section 2): every shape fits its bounds, keeps the doorway open, holds the Maw inside, and
 * keeps the root mechanic: the Maw is wrapped in roots, nothing reaches it until some are cut, and the way in leaves most of them standing.
 */
class MawShapesTest {
    private static final Kind[] PLANNED = {Kind.TOWER, Kind.RUINS, Kind.GATE};
    private static final Style[] STYLES = {MawShapes.PLAIN, new Style(55, 10), new Style(3, 35), new Style(0, 0), new Style(100, 0)};

    /** A spread of seeds, small and huge and negative. */
    private static long[] seeds() {
        long[] s = new long[48];
        for (int i = 0; i < s.length; i++) s[i] = i < 24 ? i : (i - 23) * 0x9E3779B97F4A7C15L ^ (i * -7919L);
        return s;
    }

    private static List<Plan> everyPlan, someOfThePlans;

    /** Every kind, every turn, 48 seeds, 5 styles (plans never change, so they are made once). */
    private static synchronized List<Plan> allPlans() {
        if (everyPlan == null) {
            List<Plan> out = new ArrayList<>();
            for (Kind k : PLANNED) for (int rot = 0; rot < 4; rot++) for (long seed : seeds()) for (Style st : STYLES) out.add(MawShapes.plan(k, rot, seed, st));
            everyPlan = out;
        }
        return everyPlan;
    }

    /** The same with 24 seeds and 2 styles, for the checks that walk a grid for every plan. */
    private static synchronized List<Plan> somePlans() {
        if (someOfThePlans == null) {
            List<Plan> out = new ArrayList<>();
            long[] seeds = seeds();
            for (Kind k : PLANNED) for (int rot = 0; rot < 4; rot++) for (int i = 0; i < 24; i++) for (int st = 0; st < 2; st++) out.add(MawShapes.plan(k, rot, seeds[i * 2], STYLES[st]));
            someOfThePlans = out;
        }
        return someOfThePlans;
    }

    // ---------------------------------------------------------------- a plan laid out on a grid (cells outside any put: air above the ground, ground below it)
    private static final int R = 7, BOTTOM = -7, TOP = 16, HEIGHT = TOP - BOTTOM + 1;

    private static final class Grid {
        private final Put[] cells = new Put[(2 * R + 1) * HEIGHT * (2 * R + 1)];

        Grid(Plan p) { for (Put q : p.puts()) cells[index(q.x(), q.y(), q.z())] = q; }

        private static int index(int x, int y, int z) { return ((x + R) * HEIGHT + (y - BOTTOM)) * (2 * R + 1) + (z + R); }

        static boolean inside(int x, int y, int z) { return Math.abs(x) <= R && Math.abs(z) <= R && y >= BOTTOM && y <= TOP; }

        Put at(int x, int y, int z) { return inside(x, y, z) ? cells[index(x, y, z)] : null; }

        /** Nothing is built in this cell (it may be cleared). */
        boolean free(int x, int y, int z) {
            Put q = at(x, y, z);
            return q == null || q.mat() == Mat.AIR;
        }

        /** Stone is solid, roots while {@code rootsSolid}, the ground below the surface unless the plan clears it. */
        boolean solid(int x, int y, int z, boolean rootsSolid) {
            Put q = at(x, y, z);
            if (q == null) return y < 0;
            return q.mat().stone() || (q.mat() == Mat.ROOT && rootsSolid);
        }

        boolean ladder(int x, int y, int z) {
            Put q = at(x, y, z);
            return q != null && q.mat() == Mat.LADDER;
        }

        int root(int x, int y, int z) {
            Put q = at(x, y, z);
            return q != null && q.mat() == Mat.ROOT ? 1 : 0;
        }
    }

    // ---------------------------------------------------------------- the basics
    @Test
    void theSameArgumentsGiveTheSameShape() {
        for (Kind k : PLANNED) {
            for (int rot = 0; rot < 4; rot++) {
                for (long seed : seeds()) {
                    Plan a = MawShapes.plan(k, rot, seed, MawShapes.PLAIN), b = MawShapes.plan(k, rot, seed, MawShapes.PLAIN);
                    assertEquals(a.puts(), b.puts(), k + " " + rot + " " + seed);
                    assertEquals(a.maw(), b.maw());
                    assertEquals(a.roots(), b.roots());
                    assertArrayEquals(a.columns(), b.columns());
                    assertArrayEquals(a.footings(), b.footings());
                }
            }
        }
    }

    @Test
    void otherSeedsGiveOtherShapes() {
        for (Kind k : PLANNED) {
            Set<List<Put>> distinct = new HashSet<>();
            for (long seed : seeds()) distinct.add(MawShapes.plan(k, 0, seed, MawShapes.PLAIN).puts());
            assertTrue(distinct.size() >= 40, k + " has " + distinct.size() + " different looks in 48 seeds");
        }
    }

    @Test
    void theHoleHasNoPlanAndTheOtherKindsDo() {
        assertThrows(IllegalArgumentException.class, () -> MawShapes.plan(Kind.HOLE, 0, 1, MawShapes.PLAIN));
        assertFalse(Kind.HOLE.planned());
        for (Kind k : PLANNED) assertTrue(k.planned());
    }

    @Test
    void theOrderTriesTheChosenShapeFirstAndThenTheOthers() {
        for (Kind first : PLANNED) {
            Kind[] o = MawShapes.order(first);
            assertEquals(3, o.length);
            assertEquals(first, o[0]);
            assertEquals(Set.of(PLANNED), Set.of(o), "all three, once each");
        }
        assertEquals(0, MawShapes.order(Kind.HOLE).length);
    }

    @Test
    void everyShapeFitsItsBounds() {
        for (Plan p : allPlans()) {
            String what = p.kind + " rot " + p.rot;
            assertTrue(p.puts().size() > 50 && p.puts().size() <= MawShapes.MAX_PUTS, what + " writes " + p.puts().size());
            assertTrue(p.reach() <= MawShapes.MAX_REACH, what + " reach " + p.reach());
            assertTrue(p.minY() >= -3 && p.maxY() <= 12, what + " y " + p.minY() + ".." + p.maxY());
            assertTrue(p.maxSlope() >= 1 && p.maxSlope() <= MawShapes.FOOT_MAX, what);
            assertEquals(p.kind == Kind.TOWER, p.approach() != null, what + ": only the tower has a door");
            Set<Long> seen = new HashSet<>();
            for (Put q : p.puts()) {
                assertTrue(Math.abs(q.x()) <= p.reach() && Math.abs(q.z()) <= p.reach(), what + " " + q);
                assertTrue(seen.add(((long) q.x() << 40) ^ ((long) q.y() << 20) ^ q.z()), what + " writes a cell twice: " + q);
                assertTrue(q.facing() >= 0 && q.facing() <= 3);
            }
        }
    }

    @Test
    void eachKindHasItsOwnSize() {
        for (long seed : seeds()) {
            for (int rot = 0; rot < 4; rot++) {
                Plan t = MawShapes.plan(Kind.TOWER, rot, seed, MawShapes.PLAIN);
                int stone = 0, top = 0;
                for (Put q : t.puts()) {
                    if (q.mat().stone()) stone = Math.max(stone, Math.max(Math.abs(q.x()), Math.abs(q.z())));
                    if (q.mat() != Mat.AIR && q.mat() != Mat.ROOT) top = Math.max(top, q.y());
                }
                assertEquals(2, stone, "a tower is 5x5");
                assertEquals(3, t.reach(), "and the doorstep in front of it is cleared");
                assertTrue(top + 1 >= 8 && top + 1 <= 11, "a tower stands 8 to 11 blocks: " + (top + 1));
                assertEquals(3, MawShapes.plan(Kind.GATE, rot, seed, MawShapes.PLAIN).reach(), "a gate is 7 wide, 3 deep");
                assertEquals(4, MawShapes.plan(Kind.RUINS, rot, seed, MawShapes.PLAIN).reach(), "ruins are 9x9");
            }
        }
    }

    @Test
    void theHeightOfATowerVaries() {
        Set<Integer> tops = new HashSet<>();
        for (long seed : seeds()) {
            int top = 0;
            for (Put q : MawShapes.plan(Kind.TOWER, 0, seed, MawShapes.PLAIN).puts()) if (q.mat().stone()) top = Math.max(top, q.y());
            tops.add(top + 1);
        }
        assertEquals(Set.of(8, 9, 10, 11), tops);
    }

    // ---------------------------------------------------------------- the Maw tile and its roots
    @Test
    void theMawTileIsInsideAndStandsOnStone() {
        for (Plan p : allPlans()) {
            Grid g = new Grid(p);
            Put maw = p.maw();
            assertEquals(Mat.MAW, maw.mat());
            assertSame(maw, g.at(maw.x(), maw.y(), maw.z()));
            assertTrue(Math.abs(maw.x()) <= 1 && Math.abs(maw.z()) <= 1, p.kind + " the mouth is in the middle: " + maw);
            assertEquals(1, p.puts().stream().filter(q -> q.mat() == Mat.MAW).count(), "exactly one mouth");
            Put below = g.at(maw.x(), maw.y() - 1, maw.z());
            assertNotNull(below, p.kind + " the mouth has a floor");
            assertTrue(below.mat().stone(), p.kind + " floor " + below);
            assertEquals(p.puts().get(p.puts().size() - 1), maw, "the mouth is written last");
        }
    }

    @Test
    void theMawIsWrappedInRootsOrStone() {
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}};
        for (Plan p : allPlans()) {
            Grid g = new Grid(p);
            Put maw = p.maw();
            int rootSides = 0;
            for (int[] d : dirs) {
                Put n = g.at(maw.x() + d[0], maw.y() + d[1], maw.z() + d[2]);
                assertNotNull(n, p.kind + " leaves a side of the mouth open: " + Arrays.toString(d));
                assertTrue(n.mat() == Mat.ROOT || n.mat().stone(), p.kind + " side " + n);
                if (d[1] == 1) assertEquals(Mat.ROOT, n.mat(), "the cap over the mouth is a root");
                if (n.mat() == Mat.ROOT) rootSides++;
            }
            assertTrue(rootSides >= 4, p.kind + " at least the cap and three sides are roots: " + rootSides);
        }
    }

    @Test
    void thereAreManyMoreRootsThanTheWayInNeeds() {
        for (Plan p : allPlans()) {
            int roots = (int) p.puts().stream().filter(q -> q.mat() == Mat.ROOT).count();
            assertEquals(roots, p.roots());
            assertTrue(roots >= 12 && roots <= 45, p.kind + " has " + roots + " roots");
        }
    }

    // ---------------------------------------------------------------- the root mechanic: sealed until cut, enterable once cut
    @Test
    void nothingReachesTheMawWhileTheRootsStand() {
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}};
        for (Plan p : somePlans()) {
            Grid g = new Grid(p);
            Put maw = p.maw();
            boolean[][][] seen = new boolean[2 * R + 1][HEIGHT][2 * R + 1];
            ArrayDeque<int[]> q = new ArrayDeque<>();
            for (int x = -R; x <= R; x++) {
                for (int z = -R; z <= R; z++) {
                    for (int y = BOTTOM; y <= TOP; y++) {
                        boolean shell = Math.abs(x) == R || Math.abs(z) == R || y == TOP || y == BOTTOM;
                        if (shell && !g.solid(x, y, z, true)) { seen[x + R][y - BOTTOM][z + R] = true; q.add(new int[]{x, y, z}); }
                    }
                }
            }
            while (!q.isEmpty()) {
                int[] c = q.poll();
                for (int[] d : dirs) {
                    int x = c[0] + d[0], y = c[1] + d[1], z = c[2] + d[2];
                    if (!Grid.inside(x, y, z) || g.solid(x, y, z, true) || seen[x + R][y - BOTTOM][z + R]) continue;
                    seen[x + R][y - BOTTOM][z + R] = true;
                    q.add(new int[]{x, y, z});
                }
            }
            assertFalse(seen[maw.x() + R][maw.y() - BOTTOM][maw.z() + R], p.kind + " rot " + p.rot + ": something walks to the mouth without cutting a root");
        }
    }

    /** Can a body be in this cell: not inside stone or ground (a root can be cut away). */
    private static boolean open(Grid g, int x, int y, int z) { return Grid.inside(x, y, z) && !g.solid(x, y, z, false); }

    /** Does something hold a body standing with its feet in this cell: a block under it (a root is one), or a ladder. */
    private static boolean held(Grid g, int x, int y, int z) { return g.solid(x, y - 1, z, true) || g.ladder(x, y, z); }

    /**
     * The fewest roots a walker must cut to stand in the mouth, or Integer.MAX_VALUE. A body is two cells high; it walks, steps up one block,
     * falls when nothing holds it, cuts the root under its feet to drop in, and climbs ladders. Every root in a cell it enters is one cut.
     */
    private static int fewestCuts(Plan p) {
        Grid g = new Grid(p);
        Put maw = p.maw();
        int[][][] best = new int[2 * R + 1][HEIGHT][2 * R + 1];
        for (int[][] a : best) for (int[] b : a) Arrays.fill(b, Integer.MAX_VALUE);
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[3], b[3]));
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                if (Math.abs(x) != R && Math.abs(z) != R) continue;
                best[x + R][0 - BOTTOM][z + R] = 0;
                pq.add(new int[]{x, 0, z, 0});
            }
        }
        int[][] around = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!pq.isEmpty()) {
            int[] s = pq.poll();
            int x = s[0], y = s[1], z = s[2], cost = s[3];
            if (best[x + R][y - BOTTOM][z + R] < cost) continue;
            if (x == maw.x() && y == maw.y() && z == maw.z()) return cost;
            List<int[]> next = new ArrayList<>();                                  // {x, y, z, what it costs to enter}
            for (int[] a : around) {
                int nx = x + a[0], nz = z + a[1];
                if (open(g, nx, y, nz) && open(g, nx, y + 1, nz)) next.add(new int[]{nx, y, nz, g.root(nx, y, nz) + g.root(nx, y + 1, nz)});
                if (open(g, x, y + 2, z) && open(g, nx, y + 1, nz) && open(g, nx, y + 2, nz)) next.add(new int[]{nx, y + 1, nz, g.root(nx, y + 1, nz) + g.root(nx, y + 2, nz)});   // a step up
            }
            if (open(g, x, y - 1, z)) next.add(new int[]{x, y - 1, z, g.root(x, y - 1, z)});                    // drop in, cutting the root under the feet if there is one
            if (g.ladder(x, y, z) && open(g, x, y + 2, z)) next.add(new int[]{x, y + 1, z, g.root(x, y + 2, z)});
            for (int[] n : next) {
                int nx = n[0], ny = n[1], nz = n[2], paid = cost + n[3];
                boolean inMouth = nx == maw.x() && nz == maw.z();
                while (!held(g, nx, ny, nz) && open(g, nx, ny - 1, nz) && !(inMouth && ny == maw.y())) ny--;       // nothing holds it: it falls (into the mouth, it stops)
                if (!held(g, nx, ny, nz) && !(inMouth && ny == maw.y())) continue;
                if (paid < best[nx + R][ny - BOTTOM][nz + R]) {
                    best[nx + R][ny - BOTTOM][nz + R] = paid;
                    pq.add(new int[]{nx, ny, nz, paid});
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    @Test
    void cuttingAGapLetsYouInAndLeavesMostRootsStanding() {
        for (Plan p : somePlans()) {
            int cuts = fewestCuts(p);
            assertNotEquals(Integer.MAX_VALUE, cuts, p.kind + " rot " + p.rot + ": the mouth cannot be reached even with every root cut");
            assertTrue(cuts >= (p.kind == Kind.RUINS ? 1 : 2), p.kind + " the roots are an obstacle (the ruins are open to the sky, so the cap can be cut from above): " + cuts);
            assertTrue(cuts <= 8, p.kind + " the way in is short: " + cuts);
            assertTrue(p.roots() - cuts >= 8, p.kind + " the Maw would close as soon as you got in: " + p.roots() + " roots, " + cuts + " cut");
        }
    }

    // ---------------------------------------------------------------- the tower
    @Test
    void theTowerDoorwayIsOpenAndPluggedWithRoots() {
        for (long seed : seeds()) {
            Plan p = MawShapes.plan(Kind.TOWER, 0, seed, MawShapes.PLAIN);
            Grid g = new Grid(p);
            assertEquals(Mat.ROOT, g.at(0, 0, 2).mat(), "the doorway is roots, not stone");
            assertEquals(Mat.ROOT, g.at(0, 1, 2).mat());
            assertTrue(g.at(0, 2, 2).mat().stone(), "a lintel over the doorway");
            assertTrue(g.at(-1, 0, 2).mat().stone() && g.at(1, 0, 2).mat().stone() && g.at(-1, 1, 2).mat().stone() && g.at(1, 1, 2).mat().stone(), "jambs");
            assertTrue(g.free(0, 0, 1) && g.free(0, 1, 1), "the way inside the door is free");
            assertTrue(g.free(0, 0, 3) && g.free(0, 1, 3), "and outside");
            assertTrue(g.at(0, -1, 2).mat().stone(), "a threshold");
        }
    }

    @Test
    void theApproachIsTheColumnInFrontOfTheDoor() {
        int[][] expected = {{0, 3}, {-3, 0}, {0, -3}, {3, 0}};                              // the door faces south at rot 0 and turns clockwise: west, north, east
        for (int rot = 0; rot < 4; rot++) {
            for (long seed : seeds()) {
                Plan p = MawShapes.plan(Kind.TOWER, rot, seed, MawShapes.PLAIN);
                int[] a = p.approach();
                assertArrayEquals(expected[rot], a, "rot " + rot);
                Set<String> cols = new HashSet<>();
                int[] c = p.columns();
                for (int i = 0; i < c.length; i += 2) cols.add(c[i] + "," + c[i + 1]);
                assertTrue(cols.contains(a[0] + "," + a[1]), "the approach is one of the columns the glue measures, and the loaded-chunk check reaches it");
                Put sill = new Grid(p).at(a[0] / 3 * 2, 0, a[1] / 3 * 2);                  // the doorway cell in the wall, two blocks out
                assertNotNull(sill);
                assertEquals(Mat.ROOT, sill.mat(), "the doorway is in front of the approach");
            }
        }
        assertNull(MawShapes.plan(Kind.GATE, 0, 1, MawShapes.PLAIN).approach());
        assertNull(MawShapes.plan(Kind.RUINS, 0, 1, MawShapes.PLAIN).approach());
    }

    @Test
    void theTowerHasAnInsideLadderToItsDeck() {
        int[][] backs = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}};                                 // the wall behind a ladder facing north, east, south, west: the cell it faces away from
        for (int rot = 0; rot < 4; rot++) {
            for (long seed : seeds()) {
                Plan p = MawShapes.plan(Kind.TOWER, rot, seed, MawShapes.PLAIN);
                Grid g = new Grid(p);
                List<Put> ladder = p.puts().stream().filter(q -> q.mat() == Mat.LADDER).toList();
                assertTrue(ladder.size() >= 8 && ladder.size() <= 11, "a ladder " + ladder.size());
                int x = ladder.get(0).x(), z = ladder.get(0).z();
                int top = Integer.MIN_VALUE, bottom = Integer.MAX_VALUE;
                for (Put l : ladder) {
                    assertEquals(x, l.x());
                    assertEquals(z, l.z());
                    assertEquals(ladder.get(0).facing(), l.facing());
                    top = Math.max(top, l.y());
                    bottom = Math.min(bottom, l.y());
                    int[] back = backs[l.facing()];
                    Put wall = g.at(l.x() + back[0], l.y(), l.z() + back[1]);
                    assertNotNull(wall, "the ladder is held by a wall at " + l);
                    assertTrue(wall.mat().stone(), "a ladder hangs on stone: " + l);
                }
                assertEquals(0, bottom, "it starts at the floor");
                assertEquals(ladder.size() - 1, top - bottom, "it has no gap");
                Put hatch = g.at(x, top - 1, z);
                assertNotNull(hatch, "the hatch is in the deck");
                assertEquals(Mat.LADDER, hatch.mat());
                assertTrue(g.at(x - 1, top - 1, z) != null || g.at(x + 1, top - 1, z) != null || g.at(x, top - 1, z - 1) != null || g.at(x, top - 1, z + 1) != null, "the deck is round the hatch");
                assertEquals((MawShapes.WEST + rot) & 3, ladder.get(0).facing(), "the ladder turns with the tower");
            }
        }
    }

    @Test
    void theTowerKeepsItsMawBehindTheFrontRoots() {
        for (long seed : seeds()) {
            Plan p = MawShapes.plan(Kind.TOWER, 0, seed, MawShapes.PLAIN);
            Grid g = new Grid(p);
            Put maw = p.maw();
            assertEquals(0, maw.x());
            assertEquals(-1, maw.z());
            assertEquals(Mat.ROOT, g.at(0, 0, 0).mat(), "the front root");
            assertEquals(Mat.ROOT, g.at(0, 1, 0).mat());
            assertEquals(Mat.ROOT, g.at(-1, 0, -1).mat());
            assertEquals(Mat.ROOT, g.at(1, 0, -1).mat());
            assertEquals(Mat.ROOT, g.at(0, 1, -1).mat());
            assertTrue(g.at(0, 0, -2).mat().stone(), "the back wall");
        }
    }

    // ---------------------------------------------------------------- the gate
    @Test
    void theGateIsTwoPillarsAndALintelWithTheMawInTheFrame() {
        for (long seed : seeds()) {
            Plan p = MawShapes.plan(Kind.GATE, 0, seed, MawShapes.PLAIN);
            Grid g = new Grid(p);
            for (int s : new int[]{-2, 2}) for (int y = 0; y <= 4; y++) assertTrue(g.at(s, y, 0).mat().stone(), "pillar " + s + " at " + y);
            for (int x = -2; x <= 2; x++) assertTrue(g.at(x, 5, 0).mat().stone(), "lintel " + x);
            assertEquals(0, p.maw().x());
            assertEquals(0, p.maw().y());
            assertEquals(0, p.maw().z());
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 4; y++) {
                    Put q = g.at(x, y, 0);
                    assertTrue(q == p.maw() || q == null || q.mat() == Mat.AIR || q.mat() == Mat.ROOT, "the frame holds only roots and the mouth: " + q);
                }
            }
            assertEquals(Mat.ROOT, g.at(0, 0, 1).mat(), "roots across the front");
            assertEquals(Mat.ROOT, g.at(0, 0, -1).mat(), "and the back");
        }
    }

    @Test
    void aGateTurnsSoItCanFaceAnyWay() {
        Set<Boolean> pillarsEastWest = new HashSet<>();
        for (int rot = 0; rot < 4; rot++) {
            Put east = new Grid(MawShapes.plan(Kind.GATE, rot, 3, MawShapes.PLAIN)).at(2, 0, 0);
            pillarsEastWest.add(east != null && east.mat().stone());
        }
        assertEquals(Set.of(true, false), pillarsEastWest, "pillars east-west in two turns, north-south in the others");
    }

    @Test
    void theStairsOfTheGateHugTheFootOfEachPillar() {
        for (int rot = 0; rot < 4; rot++) {
            Plan p = MawShapes.plan(Kind.GATE, rot, 5, MawShapes.PLAIN);
            Grid g = new Grid(p);
            int[][] fwd = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};                               // where a stair facing north, east, south, west climbs towards
            int stairs = 0;
            for (Put q : p.puts()) {
                if (q.mat() != Mat.STAIR) continue;
                stairs++;
                Put high = g.at(q.x() + fwd[q.facing()][0], q.y(), q.z() + fwd[q.facing()][1]);
                assertNotNull(high, "a stair climbs against something: " + q);
                assertTrue(high.mat().stone(), "the tall side of a stair is the pillar: " + q);
            }
            assertEquals(4, stairs);
        }
    }

    // ---------------------------------------------------------------- the ruins
    @Test
    void theRuinsAreAHalfSunkenChamber() {
        for (long seed : seeds()) {
            Plan p = MawShapes.plan(Kind.RUINS, 0, seed, MawShapes.PLAIN);
            Grid g = new Grid(p);
            assertEquals(-2, p.maw().y(), "two blocks into the ground");
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) assertTrue(g.at(x, -3, z).mat().stone(), "the chamber floor");
            long walls = p.puts().stream().filter(q -> q.mat().stone() && Math.max(Math.abs(q.x()), Math.abs(q.z())) == 2 && q.y() >= -2).count();
            assertTrue(walls >= 30, "the chamber is walled: " + walls);
            assertTrue(p.puts().stream().anyMatch(q -> q.mat().stone() && Math.max(Math.abs(q.x()), Math.abs(q.z())) == 2 && q.y() >= 1), "pieces of the wall stand above the ground");
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Put q = g.at(x, -2, z);
                    assertTrue(q == p.maw() || q.mat() == Mat.ROOT, "the shaft floor is the mouth under a cocoon of roots: " + q);
                }
            }
            assertEquals(-3, p.minY(), "nothing is cut deeper than the floor");
        }
    }

    @Test
    void theRuinsHaveABreachWithRootsLyingOverIt() {
        for (long seed : seeds()) {
            Plan p = MawShapes.plan(Kind.RUINS, 0, seed, MawShapes.PLAIN);
            Grid g = new Grid(p);
            int sides = 0;
            for (int[] c : new int[][]{{0, -2}, {2, 0}, {0, 2}, {-2, 0}}) {
                Put low = g.at(c[0], -1, c[1]), over = g.at(c[0], 0, c[1]);
                if (low != null && low.mat().stone() && over != null && over.mat() == Mat.ROOT) sides++;
            }
            assertEquals(1, sides, "one side of the wall is down to the ground with roots over the gap");
        }
    }

    // ---------------------------------------------------------------- order, footings, turning
    @Test
    void theBlocksComeInWritingOrder() {
        for (Plan p : allPlans()) {
            int phase = 0, lastStoneY = Integer.MIN_VALUE;
            for (Put q : p.puts()) {
                int mine = q.mat() == Mat.AIR ? 0 : q.mat() == Mat.ROOT ? 2 : q.mat() == Mat.MAW ? 3 : 1;
                assertTrue(mine >= phase, p.kind + ": " + q + " comes after a later phase");
                if (mine != phase) lastStoneY = Integer.MIN_VALUE;
                phase = mine;
                if (mine == 1) {
                    assertTrue(q.y() >= lastStoneY, p.kind + ": stone is written from the bottom up");
                    lastStoneY = q.y();
                }
            }
        }
    }

    @Test
    void everyColumnOfStoneHasAFootingEntryAtItsLowestBlock() {
        for (Plan p : allPlans()) {
            Map<String, Integer> low = new HashMap<>();
            for (Put q : p.puts()) if (q.mat().stone()) low.merge(q.x() + "," + q.z(), q.y(), Math::min);
            int[] f = p.footings();
            assertEquals(low.size() * 3, f.length);
            for (int i = 0; i < f.length; i += 3) assertEquals(low.get(f[i] + "," + f[i + 1]), f[i + 2], p.kind + " column " + f[i] + "," + f[i + 1]);
            Set<String> cols = new HashSet<>();
            for (Put q : p.puts()) cols.add(q.x() + "," + q.z());
            int[] c = p.columns();
            assertEquals(cols.size() * 2, c.length);
            for (int i = 0; i < c.length; i += 2) assertTrue(cols.contains(c[i] + "," + c[i + 1]));
            assertTrue(f.length > 0);
        }
    }

    @Test
    void turningATurnsEveryBlockAndEveryFacing() {
        // a quarter turn clockwise: (x, z) -> (-z, x), north -> east -> south -> west
        for (Kind k : PLANNED) {
            for (long seed : seeds()) {
                Plan base = MawShapes.plan(k, 0, seed, MawShapes.PLAIN);
                for (int rot = 1; rot < 4; rot++) {
                    Plan turned = MawShapes.plan(k, rot, seed, MawShapes.PLAIN);
                    Grid g = new Grid(turned);
                    assertEquals(base.puts().size(), turned.puts().size());
                    for (Put q : base.puts()) {
                        int x = q.x(), z = q.z();
                        for (int i = 0; i < rot; i++) { int nx = -z; z = x; x = nx; }
                        Put t = g.at(x, q.y(), z);
                        assertNotNull(t, k + " rot " + rot + " lost " + q);
                        assertEquals(q.mat(), t.mat());
                        assertEquals((q.facing() + rot) & 3, t.facing(), k + " " + q);
                        assertEquals(q.flip(), t.flip());
                    }
                }
            }
        }
    }

    @Test
    void wornStoneFollowsTheStyle() {
        int mossyWet = 0, brickWet = 0, mossyDry = 0, brickDry = 0, crackedDry = 0;
        for (long seed : seeds()) {
            for (Kind k : PLANNED) {
                for (Put q : MawShapes.plan(k, 0, seed, new Style(55, 10)).puts()) {
                    if (q.mat() == Mat.MOSSY) { mossyWet++; brickWet++; } else if (q.mat() == Mat.BRICK || q.mat() == Mat.CRACKED) brickWet++;
                }
                for (Put q : MawShapes.plan(k, 0, seed, new Style(3, 35)).puts()) {
                    if (q.mat() == Mat.MOSSY) { mossyDry++; brickDry++; } else if (q.mat() == Mat.CRACKED) { crackedDry++; brickDry++; } else if (q.mat() == Mat.BRICK) brickDry++;
                }
            }
        }
        assertEquals(0.55, mossyWet / (double) brickWet, 0.06);
        assertEquals(0.03, mossyDry / (double) brickDry, 0.03);
        assertTrue(crackedDry / (double) brickDry > 0.25, "a dry place cracks");
        for (Kind k : PLANNED) {
            long moss = MawShapes.plan(k, 0, 9, new Style(0, 0)).puts().stream().filter(q -> q.mat() == Mat.MOSSY).count();
            assertEquals(0, moss, "no moss when there is none");
        }
    }

    @Test
    void aStyleCannotAskForMoreThanAllTheStone() {
        assertThrows(IllegalArgumentException.class, () -> new Style(70, 40));
        assertThrows(IllegalArgumentException.class, () -> new Style(-1, 10));
        assertDoesNotThrow(() -> new Style(60, 40));
    }
}
